package com.dft.onyxdemorestclient;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by wlucas on 10/16/16.
 */

public class OnyxNode {
    public static class ComputeNfiqRequest {
        final String image;
        final int ppi;
        final int opts;

        ComputeNfiqRequest(String image, int ppi, int opts) {
            this.image = image;
            this.ppi = ppi;
            this.opts = opts;
        }
    }

    public static class ComputeNfiqResponse {
        final double nfiqScore;
        final double mlpScore;

        ComputeNfiqResponse(double nfiqScore, double mlpScore) {
            this.nfiqScore = nfiqScore;
            this.mlpScore = mlpScore;
        }
    }

    public static class PyramidImageRequest {
        final String image;
        final double[] scales;

        PyramidImageRequest(String image, double[] scales) {
            this.image = image;
            this.scales = scales;
        }
    }

    public static class PyramidImageResponse {
        final String[] imagePyramid;

        PyramidImageResponse(String[] imagePyramid) {
            this.imagePyramid = imagePyramid;
        }
    }

    public interface OnyxNodeService {
        @POST("api/methods/computenfiq")
        Call<ComputeNfiqResponse> computeNfiq(@Body ComputeNfiqRequest request);

        @POST("api/methods/pyramidimage")
        Call<PyramidImageResponse> pyramidImage(@Body PyramidImageRequest request);
    }

    private static OnyxNodeService service;
    public static OnyxNodeService getOnyxNodeService(String baseUrl) {
        if(service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(OnyxNodeService.class);
        }

        return service;
    }

}
