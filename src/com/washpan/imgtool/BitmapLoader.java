package com.washpan.imgtool;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import android.graphics.Bitmap;
import android.os.SystemClock;

/**
 * @author washpan 190848972@qq.com 用于加载bitmap的装载器
 * */
public class BitmapLoader {

	/**
	 * 网络地址
	 * */
	public static final int URI_TYPE_URL = 0;
	/**
	 * 本地文件名,不含路径
	 * */
	public static final int URI_TYPE_FILE_NAME = 1;
	/**
	 * 本地文件名,包含全路径
	 * */
	public static final int URI_TYPE_FILE_PATH = 2;

	private static final Object LOCK_INIT = new Object();
	private static BitmapLoader instance = null;

	// MB
	private static final int DEFAULT_CACHE_SIZE = 1024 * 1024 * 8;
	private BitmapLruCache cache = null;
	private int cacheSize = DEFAULT_CACHE_SIZE;
	private String path = "";
	private volatile int defaultWidth = 100;
	private volatile int defaultHeight = 100;
	private Thread loadingThread = null;
	private Worker worker = null;
	// private ArrayList<LoadBitmapListener> listeners = new
	// ArrayList<BitmapLoader.LoadBitmapListener>();
	/**
	 * 通过view找到正在执行的task,像listView重用item时快速滑动时旧的view所对应的task如果未启动就没有必要让其再启动了,
	 * 因为会很耗资源
	 * */
	private HashMap<String, LoadingTask> views = new HashMap<String, LoadingTask>();

	/**
	 * 加载锁队列
	 * */
	private LinkedBlockingQueue<LoadingTask> tasks = new LinkedBlockingQueue<BitmapLoader.LoadingTask>();

	public void setWidth(int width) {
		this.defaultWidth = width;
	}

	public void setHeight(int height) {
		this.defaultHeight = height;
	}

	class LoadingTask implements Runnable {
		private String url = "";
		private String viewKey = null;
		public boolean isWorking = false;
		private LoadBitmapListener listener = null;

		public LoadingTask(String url, String viewKey,
				LoadBitmapListener listener) {
			this.url = url;
			this.viewKey = viewKey;
			this.listener = listener;
		}

		public boolean isStop = false;

		@Override
		public void run() {

			if (isStop) {
				return;
			}
			isWorking = true;
			// 先从本地加载
			SafeBitmap safeBitmap = loadLocal(url, cache, URI_TYPE_URL);
			if (null != safeBitmap && null != safeBitmap.bitmap
					&& null != safeBitmap.bitmap.get()) {
				// 放入缓存中
				synchronized (cache) {
					cache.put(NameUtils.generateKey(url), safeBitmap);
				}
				if (null != listener && !isStop) {
					listener.onFinish(safeBitmap);
				}
			}
			// 从网络加载
			else {
				HttpDownloadPic dowload = new HttpDownloadPic();
				File cacheFile = new File(path, NameUtils.generateKey(url));
				try {
					dowload.downloadPic(cacheFile, url);

					if (null != cacheFile && cacheFile.exists()) {
						safeBitmap = loadLocal(url, cache, URI_TYPE_URL);
						if (null != safeBitmap && null != safeBitmap.bitmap) {
							// 放入缓存中
							synchronized (cache) {
								cache.put(NameUtils.generateKey(url),
										safeBitmap);
							}
							if (null != listener && !isStop) {
								listener.onFinish(safeBitmap);
							}
						}
					} else {
						if (null != listener && !isStop) {
							listener.onError();
						}
					}
				} catch (IOException e) {
					if (null != listener && !isStop) {
						listener.onError();
					}
				} finally {
					synchronized (views) {
						views.remove(viewKey);
					}
				}
			}

		}

	}

	public static BitmapLoader getInstance() {
		synchronized (LOCK_INIT) {
			if (null == instance) {
				instance = new BitmapLoader();
			}
		}
		return instance;
	}

	private BitmapLoader() {

	}

	/**
	 * 初始化方法
	 * 
	 * @param cacheSize
	 *            大小 如 8*1024*1024为8m
	 * @param path
	 *            本地缓存存放的路径
	 * @param defaultWidth
	 *            默认宽度
	 * @param defaultHeight
	 *            默认高度
	 * */
	public BitmapLoader init(int cacheSize, String path, int defaultWidth,
			int defaultHeight) {
		// 分配内存过小
		if (DEFAULT_CACHE_SIZE > cacheSize) {
			this.cacheSize = DEFAULT_CACHE_SIZE;
		}
		this.cacheSize = cacheSize;
		BitmapLruCache cache = new BitmapLruCache(cacheSize);
		this.cache = cache;
		this.path = path;
		this.defaultWidth = defaultWidth;
		this.defaultHeight = defaultHeight;
		initCache(cache);
		return this;
	}

	private void initCache(BitmapLruCache cache) {
		this.cache = cache;
		worker = new Worker();
		loadingThread = new Thread(worker);
		loadingThread.start();
	}

	private void initCache(int cacheSize) {
		BitmapLruCache cache = new BitmapLruCache(cacheSize);
		this.cache = cache;
		worker = new Worker();
		loadingThread = new Thread(worker);
		loadingThread.start();
	}

	public void removeCache(String key) {
		if (null != cache) {
			synchronized (cache) {
				cache.remove(key);
			}
		}
	}

	public class Worker implements Runnable {
		public boolean isStop = false;

		public void run() {
			while (!isStop) {
				LoadingTask task = null;
				task = tasks.poll();
				if (null != task && !task.isStop) {
					task.run();
				}
			}
		}
	}

	public interface LoadBitmapListener {
		void onFinish(final SafeBitmap bitmap);

		void onError();
	}

	/**
	 * 从完整路径加载图片,只从本地加载
	 * 
	 * @param imgFilePath
	 *            图片全路径,包含名称和后缀
	 * */
	public synchronized void loadBitmapJustLocalByWholePath(
			final String imgFilePath, final LoadBitmapListener listener) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// 停掉之前的任务
				synchronized (views) {
					LoadingTask oldTask = views.get(NameUtils.generateKey(imgFilePath));
					if (null != oldTask && !oldTask.isWorking) {
						oldTask.isStop = true;
						synchronized (tasks) {
							tasks.remove(oldTask);
						}
					}
					views.remove(NameUtils.generateKey(imgFilePath));
				}
				SafeBitmap safeBitmap = loadLocal(imgFilePath, cache,
						URI_TYPE_FILE_PATH);
				if (null != safeBitmap && null != safeBitmap.bitmap
						&& null != safeBitmap.bitmap.get()) {
					// 放入缓存中
					synchronized (cache) {
						cache.put(NameUtils.generateKey(imgFilePath), safeBitmap);
					}
					if (null != listener) {
						listener.onFinish(safeBitmap);
					}
				} else {
					if (null != listener) {
						listener.onError();
					}
				}				
			}
		}).start();
	}

	/**
	 * 根据名称去默认cache路径加载图片,只从本地加载
	 * 
	 * @param imgFilePath
	 *            图片全路径,包含名称和后缀
	 * */
	public synchronized void loadBitmapJustLocalByName(
			final String imgFileName, final LoadBitmapListener listener) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// 停掉之前的任务
				synchronized (views) {
					LoadingTask oldTask = views.get(NameUtils.generateKey(imgFileName));
					if (null != oldTask && !oldTask.isWorking) {
						oldTask.isStop = true;
						synchronized (tasks) {
							tasks.remove(oldTask);
						}
					}
					views.remove(NameUtils.generateKey(imgFileName));
				}
				SafeBitmap safeBitmap = loadLocal(imgFileName, cache,
						URI_TYPE_FILE_NAME);
				if (null != safeBitmap && null != safeBitmap.bitmap
						&& null != safeBitmap.bitmap.get()) {
					// 放入缓存中
					synchronized (cache) {
						cache.put(NameUtils.generateKey(imgFileName), safeBitmap);
					}
					if (null != listener) {
						listener.onFinish(safeBitmap);
					}
				} else {
					if (null != listener) {
						listener.onError();
					}
				}
			}
		}).start();
	}

	/**
	 * 加载图片,先从本地加载,如果本地没有则去网络加载
	 * 
	 * @param 要加载的图片view容器
	 *            ,可以为null
	 * @param netUrl
	 *            图片链接地址
	 * */
	public synchronized void loadBitmap(final String netUrl,
			final LoadBitmapListener listener) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// 停掉之前的任务
				synchronized (views) {
					LoadingTask oldTask = views.get(NameUtils.generateKey(netUrl));
					if (null != oldTask && !oldTask.isWorking) {
						oldTask.isStop = true;
						synchronized (tasks) {
							tasks.remove(oldTask);
						}
					}
					views.remove(NameUtils.generateKey(netUrl));
				}
				SafeBitmap safeBitmap = loadLocal(netUrl, cache, URI_TYPE_URL);
				if (null != safeBitmap && null != safeBitmap.bitmap
						&& null != safeBitmap.bitmap.get()) {
					// 放入缓存中
					synchronized (cache) {
						cache.put(NameUtils.generateKey(netUrl), safeBitmap);
					}
					if (null != listener) {
						listener.onFinish(safeBitmap);
					}
					return;
				}
				LoadingTask task = new LoadingTask(netUrl,
						NameUtils.generateKey(netUrl), listener);
				synchronized (views) {
					views.put(NameUtils.generateKey(netUrl), task);
				}
				try {
					tasks.put(task);
				} catch (InterruptedException e) {
					e.printStackTrace();
					if (null != listener) {
						listener.onError();
					}
				}
			}
		}).start();
	}

	/**
	 * 从本地加载图片
	 * 
	 * @param uri
	 *            资源路径
	 * @param cache
	 *            缓存
	 * @param uriType
	 * */
	private SafeBitmap loadLocal(String uri, BitmapLruCache cache, int uriType) {

		SafeBitmap safeBitmap = new SafeBitmap();
		String filePath = uri;
		switch (uriType) {
		case URI_TYPE_URL: {
			filePath = path + File.separator + NameUtils.generateKey(uri);
		}
			break;
		case URI_TYPE_FILE_PATH: {
			filePath = uri;
		}
			break;
		case URI_TYPE_FILE_NAME: {
			filePath = path + File.separator + uri;
		}
			break;
		default:
			return null;
		}
		try {

			Bitmap bitmap = BitmapDecodeTool.decodeBitmap(filePath,
					defaultWidth, defaultHeight, 3, Bitmap.Config.RGB_565,
					false);
			if (null != bitmap) {
				safeBitmap.bitmap = new SoftReference<Bitmap>(bitmap);
				safeBitmap.config = Bitmap.Config.RGB_565;
				safeBitmap.size = BitmapDecodeTool.sizeOfBitmap(bitmap,
						safeBitmap.config);
			}
		} catch (Error e) {
			synchronized (cache) {
				cache.evictAll();
			}
			System.gc();
			SystemClock.sleep(2000);
			try {
				Bitmap bitmap = BitmapDecodeTool.decodeBitmap(filePath,
						defaultWidth, defaultHeight, 3, Bitmap.Config.RGB_565,
						false);
				if (null != bitmap) {
					safeBitmap.bitmap = new SoftReference<Bitmap>(bitmap);
					safeBitmap.config = Bitmap.Config.RGB_565;
					safeBitmap.size = BitmapDecodeTool.sizeOfBitmap(bitmap,
							safeBitmap.config);
				}
			} catch (Error e2) {
			}
		}
		return safeBitmap;
	}

	public SafeBitmap getCahe(String key) {
		return this.cache.get(key);
	}

	/**
	 * @param isRecycle
	 *            是否需要回收,注意,如果是子视图返回父视图最好进行回收,
	 * */
	public void cleanCache(boolean isRecycle) {
		this.cache.isRecycleWhenRemove = isRecycle;
		synchronized (this.cache) {
			this.cache.evictAll();
		}
		if (null != worker) {
			worker.isStop = true;
		}
		synchronized (views) {
			views.clear();
		}
		synchronized (tasks) {
			tasks.clear();
		}
		System.gc();
		SystemClock.sleep(2000);
		// 重新初始化
		initCache(cacheSize);
	}
}
