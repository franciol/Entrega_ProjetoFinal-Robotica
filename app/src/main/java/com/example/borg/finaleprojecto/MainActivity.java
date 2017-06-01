package com.example.borg.finaleprojecto;

import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static android.os.SystemClock.sleep;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.MORPH_CLOSE;
import static org.opencv.imgproc.Imgproc.RETR_LIST;
import static org.opencv.imgproc.Imgproc.RETR_TREE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_TOZERO;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.morphologyEx;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    Mat image, image1;
    private static String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Scalar cor_menor,cor_menor1,cor_maior1,cor_maior;
    Mat kernel;
    Mat mHierarchy;
    Mat maior_contorno;
    double area,a;
    List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    double maior_contorno_area;
    private Socket socket;
    private PrintWriter printWriter;
    private String message;
    int port  = 7777;

    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();

                    break;
                }
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV Loaded Succesfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        } else {
            Log.i(TAG, "OpenCV Failed to Load");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallBack);
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        image = new Mat(height, width, CV_8UC4);
        image1 = new Mat(height, width, CvType.CV_8UC1);
        kernel = getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
        maior_contorno_area = 0;
        cor_menor = new Scalar(0, 50, 50);
        cor_maior = new Scalar(8, 255, 255);
        cor_menor1 = new Scalar(172, 50, 50);
        cor_maior1 = new Scalar(180, 255, 255);
        mHierarchy = new Mat(height,width,CV_8UC4);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    Socket client = new Socket("192.168.5.105", 7777);

                    printWriter = new PrintWriter(client.getOutputStream());
                    printWriter.write("TestMode on\n");
                    printWriter.flush();
                    printWriter.close();
                    client.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        maior_contorno =null;
        maior_contorno_area = 0;
        contours.clear();
        sleep(50);
        image = inputFrame.rgba();
        a = image.width()/4;
        cvtColor(image, image, Imgproc.COLOR_RGBA2BGR);
        cvtColor(image, image, COLOR_BGR2HSV);
        Core.inRange(image.clone(), cor_menor, cor_maior, image1);
        Core.inRange(image.clone(), cor_menor1, cor_maior1, image);
        morphologyEx(image1, image1, MORPH_CLOSE, kernel);


        findContours(image1.clone(), contours, mHierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
        maior_contorno = null;
        maior_contorno_area = 0;


        for (Mat cnt : contours) {
            area = contourArea(cnt);
            if (maior_contorno_area < area) {
                maior_contorno = cnt;
                maior_contorno_area = area;
            }
        }

        if (maior_contorno != null) {
            drawContours(image1, contours,contours.indexOf(maior_contorno), new Scalar(50, 50, 50), 5);
            double i = contours.get(contours.indexOf(maior_contorno)).width() / 2;
            Rect oie = boundingRect(contours.get(contours.indexOf(maior_contorno)));
            double pos_x = oie.x + i;


            Log.i("TAG2","imagem: "+String.valueOf(a));
            Log.i("TAG2","contour: "+String.valueOf(pos_x));
            if (a*0.7 < pos_x && pos_x< a*1.3 ) {
                Log.i("TAG","FRENTE");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            Socket client = new Socket("192.168.5.105", 7777);

                            printWriter = new PrintWriter(client.getOutputStream());
                            printWriter.write("TestMode on\n");
                            printWriter.flush();
                            printWriter.write("SetMotor 100 100 100\n");
                            printWriter.flush();
                            printWriter.close();
                            client.close();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();


            }
            else if (pos_x < a*0.7) {
                Log.i("TAG","ESQUERDA");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            Socket client = new Socket("192.168.5.105", 7777);

                            printWriter = new PrintWriter(client.getOutputStream());
                            printWriter.write("TestMode on\n");
                            printWriter.flush();
                            printWriter.write("SetMotor 100 0 100\n");
                            printWriter.flush();
                            printWriter.close();
                            client.close();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();


            }
            else if (pos_x > a*1.3) {
                Log.i("TAG","DIREITA");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            Socket client = new Socket("192.168.5.105", 7777);

                            printWriter = new PrintWriter(client.getOutputStream());
                            printWriter.write("TestMode on\n");
                            printWriter.flush();
                            printWriter.write("SetMotor 0 100 100\n");
                            printWriter.flush();
                            printWriter.close();
                            client.close();
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }



        }else {
            Log.i("TAG","RODA-RODA-RODA");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        Socket client = new Socket("192.168.5.105", 7777);

                        printWriter = new PrintWriter(client.getOutputStream());
                        printWriter.write("TestMode on\n");
                        printWriter.flush();
                        printWriter.write("SetMotor 100 -100 100\n");
                        printWriter.flush();
                        printWriter.close();
                        client.close();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
        return image1;
    }

}