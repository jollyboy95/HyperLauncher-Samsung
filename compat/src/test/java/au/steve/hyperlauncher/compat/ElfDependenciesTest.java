package au.steve.hyperlauncher.compat;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ElfDependenciesTest {
    private byte[] fixture() {
        byte[] a = new byte[512]; ByteBuffer b = ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN);
        a[0]=0x7f; a[1]='E'; a[2]='L'; a[3]='F'; a[4]=2; a[5]=1;
        b.putShort(18,(short)183); b.putLong(32,64); b.putShort(54,(short)56); b.putShort(56,(short)2);
        b.putInt(64,1); b.putLong(72,0); b.putLong(80,0x1000); b.putLong(96,512);
        b.putInt(120,2); b.putLong(128,192); b.putLong(152,64);
        b.putLong(192,5); b.putLong(200,0x1120); // strtab address -> file offset 288
        b.putLong(208,10); b.putLong(216,64);
        b.putLong(224,1); b.putLong(232,1);
        byte[] name = "libhyper_os_app_public.so".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(name,0,a,289,name.length);
        return a;
    }
    @Test public void resolvesVirtualStringAddressWithoutSections() throws Exception {
        assertEquals(List.of("libhyper_os_app_public.so"), ElfDependencies.read(fixture()));
    }
    @Test public void acceptsNoDynamicDependencies() throws Exception {
        byte[] a=fixture(); ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN).putInt(120,0);
        assertTrue(ElfDependencies.read(a).isEmpty());
    }
    @Test(expected=IOException.class) public void rejectsTruncatedFile() throws Exception { ElfDependencies.read(new byte[10]); }
    @Test(expected=IOException.class) public void rejectsWrongArchitecture() throws Exception {
        byte[] a=fixture(); a[18]=62; ElfDependencies.read(a);
    }
    @Test(expected=IOException.class) public void rejectsUnmappedStringTable() throws Exception {
        byte[] a=fixture(); ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN).putLong(200,0x9000); ElfDependencies.read(a);
    }
    @Test(expected=IOException.class) public void rejectsStringOutsideTable() throws Exception {
        byte[] a=fixture(); ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN).putLong(232,64); ElfDependencies.read(a);
    }
    @Test(expected=IOException.class) public void rejectsUnterminatedString() throws Exception {
        byte[] a=fixture(); for(int i=289;i<352;i++)a[i]='x'; ElfDependencies.read(a);
    }
}
