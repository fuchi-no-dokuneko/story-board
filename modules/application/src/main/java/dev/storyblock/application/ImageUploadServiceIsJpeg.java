package dev.storyblock.application;



final class ImageUploadServiceIsJpeg {
    static boolean isJpeg(byte[] content) {
        return content.length >= 3
                && content[0] == (byte) 0xff
                && content[1] == (byte) 0xd8
                && content[2] == (byte) 0xff;
    }
}
