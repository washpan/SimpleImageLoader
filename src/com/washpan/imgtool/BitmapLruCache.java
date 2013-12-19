package com.washpan.imgtool;



/**
 * @author washpan 190848972@qq.com 用于预读Bitmap对象的LruCache
 * */
public class BitmapLruCache extends LruCache<String, SafeBitmap> {

	/**
	 * 删除时是否释放
	 * */
	public boolean isRecycleWhenRemove = false;
	public BitmapLruCache(int maxSize) {
		super(maxSize);
	}

	/**
	 * 如果是释放操作判断是否需要释放,标记释放位
	 * */
	@Override
	protected void entryRemoved(boolean evicted, String key, SafeBitmap oldValue,
			SafeBitmap newValue) {
		super.entryRemoved(evicted, key, oldValue, newValue);
		if (evicted) {
			if (null != oldValue && isRecycleWhenRemove) {
				if (null != oldValue.bitmap &&null !=oldValue.bitmap.get()&& oldValue.bitmap.get().isRecycled()) {
					oldValue.bitmap.get().recycle();
				}
			}
		}
	}

	/**
	 * 这里计算Bitmap的大小
	 * */
	@Override
	protected int sizeOf(String key, SafeBitmap value) {
		if(null == value){
			return 0;
		}
		return value.size;
	}

}
