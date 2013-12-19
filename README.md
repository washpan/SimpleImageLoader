SimpleImageLoader
=================

Load bitmap lib for android platform,see demo https://github.com/washpan/BitmapTool.git

//1 init
private BitmapLoader loader = null;
private void initCache() {
		int cacheSize = Math.round(Runtime.getRuntime().maxMemory() >> 12);
		loader = BitmapLoader.getInstance().init(cacheSize, STORE_PATH, getResources()
				.getDisplayMetrics().widthPixels, 100);
	}

//2. use
loader.loadBitmap(
						/**your url**/, new LoadBitmapListener() {

							@Override
							public void onFinish(
									final SafeBitmap bitmap) {
									//TODO something
							
							}

							@Override
							public void onError() {
								handler.post(new Runnable() {

									@Override
									public void run() {
										//TODO something
									}
								});

							}

						});
						
	//3 .clean cache on destroy or pause
	if (null != loader) {
			loader.cleanCache(false);
		}
			
