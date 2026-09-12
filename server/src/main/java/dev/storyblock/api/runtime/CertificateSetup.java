package dev.storyblock.api.runtime;

public final class CertificateSetup {
    private CertificateSetup() {}
    public static void main(String[] args) throws Exception {
        LocalServerTls.prepare();
        System.out.println("Local self-signed leaf ready / 本機自簽憑證已備妥 / 本机自签证书已就绪");
    }
}
