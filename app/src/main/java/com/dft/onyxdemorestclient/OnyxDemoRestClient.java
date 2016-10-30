package com.dft.onyxdemorestclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.dft.onyx.core;
import com.dft.onyxcamera.licensing.License;
import com.dft.onyxcamera.ui.CaptureConfiguration;
import com.dft.onyxcamera.ui.CaptureConfigurationBuilder;
import com.dft.onyxcamera.ui.CaptureMetrics;
import com.dft.onyxcamera.ui.OnyxFragment;

import org.opencv.android.OpenCVLoader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnyxDemoRestClient extends Activity {
    private final static String TAG = OnyxDemoRestClient.class.getSimpleName();
    private Activity mActivity;
    private OnyxFragment mFragment;

    public enum CommandType {
        PYRAMID_IMAGE,
        COMPUTE_NFIQ,
        GENERATE_ISO_FINGERPRINT_TEMPLATE
    }

    // TODO: make this selectable from the UI.
    CommandType commandType = CommandType.GENERATE_ISO_FINGERPRINT_TEMPLATE;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Unable to load OpenCV!");
        } else {
            Log.i(TAG, "OpenCV loaded successfully");
            core.initOnyx();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onyx_demo_rest_client);
        mActivity = this;

        mFragment = (OnyxFragment) getFragmentManager().findFragmentById(R.id.onyx_frag);
        CaptureConfiguration captureConfig = new CaptureConfigurationBuilder()
                .setWsqCallback(mWsqCallback)
                .setShouldInvert(true)
                .setFlip(CaptureConfiguration.Flip.VERTICAL)
                .buildCaptureConfiguration();
        mFragment.setCaptureConfiguration(captureConfig);
        mFragment.setErrorCallback(mErrorCallback);
        mFragment.startOneShotAutoCapture();
    }

    @Override
    public void onResume() {
        super.onResume();
        License lic = License.getInstance(this);
        try {
            lic.validate(getString(R.string.onyx_license));
        } catch (Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("License error")
                    .setMessage(e.getMessage())
                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            builder.create().show();
        }
    }

    private OnyxFragment.WsqCallback mWsqCallback = new OnyxFragment.WsqCallback() {
        @Override
        /**
         * This method handles the WsqReady event.
         * @param wsqData a byte array containing the compressed WSQ data of the fingerprint image.
         * @param metrics the metrics associated with this fingerprint image capture.
         */
        public void onWsqReady(byte[] wsqData, CaptureMetrics metrics) {
            OnyxNode.OnyxNodeService service = OnyxNode.getOnyxNodeService(getString(R.string.onyx_node_url));

            switch (commandType) {
                case PYRAMID_IMAGE: {
                    double[] scales = {0.8, 1.0, 1.2};
                    OnyxNode.PyramidImageRequest request = new OnyxNode.PyramidImageRequest(Base64.encodeToString(wsqData, Base64.DEFAULT), scales);
                    Call<OnyxNode.PyramidImageResponse> call = service.pyramidImage(request);
                    call.enqueue(new Callback<OnyxNode.PyramidImageResponse>() {

                        @Override
                        public void onResponse(Call<OnyxNode.PyramidImageResponse> call, Response<OnyxNode.PyramidImageResponse> response) {
                            String[] imagePyramidBase64 = response.body().imagePyramid;
                            handleResponseSuccess("imagePyramidBase64.length: " + imagePyramidBase64.length);

                            for (int i = 0; i < imagePyramidBase64.length; i++) {
                                byte[] ithWsqData = Base64.decode(imagePyramidBase64[i], Base64.DEFAULT);

                                // TODO: do something with WSQ pyramid level i...
                            }
                        }

                        @Override
                        public void onFailure(Call<OnyxNode.PyramidImageResponse> call, Throwable t) {
                            handleResponseFailure(t.getMessage());
                        }
                    });
                }
                break;

                case COMPUTE_NFIQ: {
                    OnyxNode.ComputeNfiqRequest request = new OnyxNode.ComputeNfiqRequest(Base64.encodeToString(wsqData, Base64.DEFAULT), 500, 0);
                    Call<OnyxNode.ComputeNfiqResponse> call = service.computeNfiq(request);
                    call.enqueue(new Callback<OnyxNode.ComputeNfiqResponse>() {

                        @Override
                        public void onResponse(Call<OnyxNode.ComputeNfiqResponse> call, Response<OnyxNode.ComputeNfiqResponse> response) {
                            handleResponseSuccess("NFIQ score: " + response.body().nfiqScore);
                        }

                        @Override
                        public void onFailure(Call<OnyxNode.ComputeNfiqResponse> call, Throwable t) {
                            handleResponseFailure(t.getMessage());
                        }
                    });
                }
                break;

                case GENERATE_ISO_FINGERPRINT_TEMPLATE: {
                    OnyxNode.GenerateIsoFingerprintTemplateRequest request = new OnyxNode.GenerateIsoFingerprintTemplateRequest(Base64.encodeToString(wsqData, Base64.DEFAULT));
                    Call<OnyxNode.GenerateIsoFingerprintTemplateResponse> call = service.generateIsoFingerprintTemplate(request);
                    call.enqueue(new Callback<OnyxNode.GenerateIsoFingerprintTemplateResponse>() {

                        @Override
                        public void onResponse(
                                Call<OnyxNode.GenerateIsoFingerprintTemplateResponse> call,
                                Response<OnyxNode.GenerateIsoFingerprintTemplateResponse> response) {
                            handleResponseSuccess("Template quality: " + response.body().quality);

                            // TODO: Use ISO fingerprint template data...
                            byte[] isoTemplate = Base64.decode(response.body().data, Base64.DEFAULT);
                        }

                        @Override
                        public void onFailure(
                                Call<OnyxNode.GenerateIsoFingerprintTemplateResponse> call,
                                Throwable t) {
                            handleResponseFailure(t.getMessage());
                        }
                    });
                }
                break;
            }
        }
    };

    private void handleResponseSuccess(String message) {
        Toast toast = Toast.makeText(
                mActivity,
                message,
                Toast.LENGTH_SHORT
        );
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
        mFragment.startOneShotAutoCapture();
    }

    private void handleResponseFailure(String message) {
        Toast toast = Toast.makeText(
                mActivity,
                message,
                Toast.LENGTH_SHORT
        );
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
        mFragment.startOneShotAutoCapture();
    }

    private OnyxFragment.ErrorCallback mErrorCallback = new OnyxFragment.ErrorCallback() {

        @Override
        /**
         * This method handles the errors that can be produced by the OnyxFragment.
         * @param error the specific error enumeration that occurred.
         * @param errorMessage the associated error message.
         * @param exception if not null, this is the exception that occurred.
         */
        public void onError(Error error, String errorMessage, Exception exception) {
            switch (error) {
                case AUTOFOCUS_FAILURE:
                    mFragment.startOneShotAutoCapture();
                    break;
                default:
                    Log.d(TAG, "Error occurred: " + errorMessage);
                    break;
            }
        }

    };
}
