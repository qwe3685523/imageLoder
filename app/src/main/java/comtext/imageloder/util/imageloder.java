package comtext.imageloder.util;

import android.graphics.Bitmap;
import android.net.MailTo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import comtext.imageloder.bean.ImageBean;

/**
 * Created by eryan on 2016/6/2.
 */
public class imageloder {
    //类的对象
    private static imageloder sImageloder;
    //缓存核心
    private LruCache<String, Bitmap> mLruCache;
    //线程池
    private ExecutorService mTheadPool;
    //默认线程数量
    private static final int DEFULT_THREAD_COUNT = 1;
    //队列调度方式
    private Type mType = Type.LIFO;

    public enum Type {
        FIFO, LIFO;
    }

    //任务队列
    private LinkedList<Runnable> mTaskQueue;

    //后台轮询线程
    private Thread mPoolThread;
    private Handler mPoolThreadhandler;


    //ui线程中的handeler
    private Handler mUIhandler;


    //私有化构造方法
    private imageloder(int mThreadcount, Type type) {
        init(mThreadcount, type);

    }

    /**
     * 初始化
     *
     * @param mThreadcount 线程池线程数
     * @param type         调度方式
     */
    private void init(int mThreadcount, Type type) {
        mPoolThread = new Thread() {
            @Override
            public void run() {

                Looper.prepare();

                mPoolThreadhandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池取出任务 执行

                        mTheadPool.execute(getTask());
                    }
                };

                Looper.loop();


            }
        };
        //开启
        mPoolThread.start();
        //获取最大内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        //获得缓存内存
        int cacheMemory = maxMemory / 8;
        //初始化
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            //测量每个bitmap的值
            @Override
            protected int sizeOf(String key, Bitmap value) {


                return value.getRowBytes() * value.getHeight();
            }
        };


        //创建线程池
        mTheadPool = Executors.newFixedThreadPool(mThreadcount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;
    }

    /**
     * 从任务队列取出任务
     *
     * @return
     */
    private Runnable getTask() {

        if (mType == Type.FIFO) {

            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();

        }
        return null;
    }


    //对外提供获取对象的方法
    public static imageloder getsImageloder() {
        if (sImageloder == null) {
            synchronized (imageloder.class) {
                if (sImageloder == null) {
                    sImageloder = new imageloder(DEFULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return null;
    }


    /**
     * 更具path 设置图片
     *
     * @param path
     * @param imageView
     */
    public void loadImage(String path, final ImageView imageView) {
        imageView.setTag(path);
        if (mUIhandler == null) {
            mUIhandler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    //获取图片 ,为image回调设置图片
                    ImageBean imageBean = (ImageBean) msg.obj;
                    Bitmap bitmap = imageBean.mBitmap;
                    ImageView imageView = imageBean.mImageView;
                    String path = imageBean.path;


                    if (imageView.getTag().toString().equals(path)) {
                        imageView.setImageBitmap(bitmap);
                    }


                }
            };

        }
        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null) {
            Message msg = Message.obtain();

            ImageBean bean = new ImageBean();
            bean.mBitmap = bm;
            bean.mImageView = imageView;
            bean.path = path;

            msg.obj = bean;
            mUIhandler.sendMessage(msg);

        } else {

            addTask(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    //压缩图片
                    //1获取图片徐显示大小
                    getImagerViewSize(imageView);



                }
            });


        }


    }

    /**
     *
     * @param imageView
     */
    private void getImagerViewSize(ImageView imageView) {
         // TODO: 2016/6/2

    }

    private void addTask(Runnable runnable) {

        mTaskQueue.add(runnable);
        //发送通知
        mPoolThreadhandler.sendEmptyMessage(0x110);
    }

    /**
     * 更具path 在lrucache  中 获取图片
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

}
