package au.steve.hyperlauncher.compat.aurora.core;

/** Converts model confidence to soft alpha; does not itself detect subjects. */
public final class MaskMath {
    private MaskMath(){}
    public static byte[] refine(float[] confidence,int w,int h){
        return refine(confidence, w, h, 0.30f);
    }
    public static byte[] refine(float[] confidence,int w,int h,float threshold){
        if(w<=0||h<=0||confidence==null||confidence.length!=w*h)throw new IllegalArgumentException("Invalid mask");
        float cutoff=Math.max(.05f,Math.min(.8f,threshold));
        float[] a=new float[w*h];
        for(int i=0;i<a.length;i++){float t=Math.max(0,Math.min(1,(confidence[i]-cutoff)/Math.max(.05f,.80f-cutoff)));a[i]=t*t*(3-2*t);}
        byte[] out=new byte[a.length];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int i=y*w+x;float v=a[i];
            if(v<.999f){float sum=0;int count=0;for(int yy=Math.max(0,y-1);yy<=Math.min(h-1,y+1);yy++)for(int xx=Math.max(0,x-1);xx<=Math.min(w-1,x+1);xx++){sum+=a[yy*w+xx];count++;}v=(v+sum/count)*.5f;}
            out[i]=(byte)Math.round(Math.min(1,v)*255);
        }
        return out;
    }
}
