package au.steve.hyperlauncher.compat.aurora.core;

/** Wallpaper-aware pastel clock colour, without Android dependencies. */
public final class ColorMath {
    private ColorMath(){}
    public static int fromSamples(int[] pixels) {
        if(pixels==null||pixels.length==0)return 0xFFD0E9FF;
        float[] bins=new float[36]; float sum=0,weight=0;
        for(int p:pixels){
            float[] hsl=toHsl((p>>16)&255,(p>>8)&255,p&255);
            float w=hsl[1]*(1-Math.abs(hsl[2]-.5f)*1.6f);
            if(w>0){bins[Math.min(35,(int)(hsl[0]/10))]+=w;weight+=w;}
            sum+=hsl[2];
        }
        int best=0;float high=-1;
        for(int i=0;i<36;i++){float v=bins[i]+.5f*(bins[(i+35)%36]+bins[(i+1)%36]);if(v>high){high=v;best=i;}}
        float avg=sum/pixels.length;
        if(weight/pixels.length<.025f)return avg>.7f?0xFF3C4149:0xFFE9ECF2;
        return fromHsl(best*10+5,.46f,avg>.72f?.25f:.70f);
    }
    private static float[] toHsl(int r,int g,int b){
        float a=r/255f,c=g/255f,d=b/255f,max=Math.max(a,Math.max(c,d)),min=Math.min(a,Math.min(c,d)),delta=max-min;
        float h=0,l=(max+min)/2,s=delta==0?0:delta/(1-Math.abs(2*l-1));
        if(delta>0){if(max==a)h=((c-d)/delta)%6;else if(max==c)h=(d-a)/delta+2;else h=(a-c)/delta+4;h=(h*60+360)%360;}
        return new float[]{h,s,l};
    }
    private static int fromHsl(float h,float s,float l){
        float c=(1-Math.abs(2*l-1))*s,x=c*(1-Math.abs((h/60)%2-1)),m=l-c/2;
        float r=0,g=0,b=0;
        if(h<60){r=c;g=x;}else if(h<120){r=x;g=c;}else if(h<180){g=c;b=x;}else if(h<240){g=x;b=c;}else if(h<300){r=x;b=c;}else{r=c;b=x;}
        return 0xFF000000|(Math.round((r+m)*255)<<16)|(Math.round((g+m)*255)<<8)|Math.round((b+m)*255);
    }
}
