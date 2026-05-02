package com.j_phone.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import remexa.host.LaunchConfig;
import remexa.host.input.HostCameraCaptureRequest;
import remexa.host.runtime.MidletRuntime;

public final class HostedCameraConnection implements CameraConnection {
    private static final int[][] AVAILABLE_SIZES = new int[][]{
            {480, 854},
            {240, 320},
            {176, 144},
            {144, 144},
            {128, 96}
    };
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private int selectedSizeId = 0;
    private int pictureQuality = QUALITY_FINE;
    private int pictureFormat = FORMAT_JPEG;
    private String frameFileName;
    private byte[] frameBytes;
    private String lastFileName;
    private boolean closed;

    @Override
    public boolean isSupported(int chkType) throws IllegalArgumentException {
        return switch (chkType) {
            case CHKTYPE_FORMAT_JPEG, CHKTYPE_FORMAT_PNG -> true;
            default -> throw new IllegalArgumentException("Unsupported camera capability check: " + chkType);
        };
    }

    @Override
    public int countAvailablePictureSizes() {
        return AVAILABLE_SIZES.length;
    }

    @Override
    public int getPictureWidth(int sizeId) throws IllegalArgumentException {
        validateSizeId(sizeId);
        return AVAILABLE_SIZES[sizeId][0];
    }

    @Override
    public int getPictureHeight(int sizeId) throws IllegalArgumentException {
        validateSizeId(sizeId);
        return AVAILABLE_SIZES[sizeId][1];
    }

    @Override
    public void setPictureSize(int sizeId) throws IllegalArgumentException {
        validateSizeId(sizeId);
        selectedSizeId = sizeId;
    }

    @Override
    public void setPictureQuality(int quality) throws IllegalArgumentException {
        if (quality != QUALITY_NORMAL && quality != QUALITY_FINE && quality != QUALITY_SUPERFINE) {
            throw new IllegalArgumentException("Unsupported camera quality: " + quality);
        }
        pictureQuality = quality;
    }

    @Override
    public void setPictureFormat(int format) throws IllegalArgumentException {
        if (format != FORMAT_JPEG && format != FORMAT_PNG) {
            throw new IllegalArgumentException("Unsupported camera format: " + format);
        }
        pictureFormat = format;
    }

    @Override
    public void setPictureFrame(String frameFileName) {
        this.frameFileName = frameFileName == null || frameFileName.isBlank() ? null : frameFileName.trim();
        this.frameBytes = null;
    }

    @Override
    public void setPictureFrame(byte[] bytes) {
        this.frameBytes = bytes == null || bytes.length == 0 ? null : bytes.clone();
        this.frameFileName = null;
    }

    @Override
    public void capture() throws IOException {
        ensureOpen();
        lastFileName = null;
        if (LaunchConfig.CameraInputMode.resolveConfigured() == LaunchConfig.CameraInputMode.DISABLED) {
            return;
        }

        int targetWidth = getPictureWidth(selectedSizeId);
        int targetHeight = getPictureHeight(selectedSizeId);
        String format = pictureFormat == FORMAT_PNG ? "png" : "jpeg";
        var capture = MidletRuntime.requestCameraCaptureResult(new HostCameraCaptureRequest(
                "Camera",
                targetWidth,
                targetHeight,
                format,
                frameBytes != null || frameFileName != null
        ));
        if (capture == null || !capture.accepted() || capture.sourcePath() == null) {
            return;
        }

        var sourceImage = ImageIO.read(capture.sourcePath().toFile());
        if (sourceImage == null) {
            throw new IOException("Selected file is not a supported image.");
        }

        BufferedImage outputImage = renderCapturedImage(sourceImage, targetWidth, targetHeight, format);
        String logicalFileName = createLogicalOutputPath(format);
        writeCapturedImage(outputImage, logicalFileName, format);
        lastFileName = logicalFileName;
    }

    @Override
    public String getFileName() {
        return lastFileName;
    }

    @Override
    public void close() {
        closed = true;
        lastFileName = null;
    }

    private BufferedImage renderCapturedImage(BufferedImage sourceImage, int targetWidth, int targetHeight, String format)
            throws IOException {
        int imageType = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        var output = new BufferedImage(targetWidth, targetHeight, imageType);
        var graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (imageType == BufferedImage.TYPE_INT_RGB) {
                graphics.setColor(Color.BLACK);
                graphics.fillRect(0, 0, targetWidth, targetHeight);
            }
            drawCoverImage(graphics, sourceImage, targetWidth, targetHeight);
            var frameImage = decodeFrameImage();
            if (frameImage != null) {
                graphics.drawImage(frameImage, 0, 0, targetWidth, targetHeight, null);
            }
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private static void drawCoverImage(Graphics2D graphics, BufferedImage sourceImage, int targetWidth, int targetHeight) {
        double scale = Math.max(
                (double) targetWidth / Math.max(1, sourceImage.getWidth()),
                (double) targetHeight / Math.max(1, sourceImage.getHeight())
        );
        int drawWidth = Math.max(1, (int) Math.round(sourceImage.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(sourceImage.getHeight() * scale));
        int drawX = (targetWidth - drawWidth) / 2;
        int drawY = (targetHeight - drawHeight) / 2;
        graphics.drawImage(sourceImage, drawX, drawY, drawWidth, drawHeight, null);
    }

    private BufferedImage decodeFrameImage() throws IOException {
        if (frameBytes != null && frameBytes.length > 0) {
            try (var input = new ByteArrayInputStream(frameBytes)) {
                return ImageIO.read(input);
            }
        }
        if (frameFileName == null || frameFileName.isBlank()) {
            return null;
        }
        String target = frameFileName.contains(":") ? frameFileName : "file://" + frameFileName;
        try (InputStream input = javax.microedition.io.Connector.openInputStream(target)) {
            return input == null ? null : ImageIO.read(input);
        }
    }

    private String createLogicalOutputPath(String format) {
        String extension = "png".equals(format) ? ".png" : ".jpg";
        String fileName = "capture-" + FILE_TIMESTAMP.format(LocalDateTime.now()) + extension;
        return "/mc/camera/" + fileName;
    }

    private void writeCapturedImage(BufferedImage image, String logicalFileName, String format) throws IOException {
        var target = StoragePathSupport.resolve(logicalFileName);
        Path realPath = target.realPath();
        Path parent = realPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if ("png".equals(format)) {
            ImageIO.write(image, "png", realPath.toFile());
            return;
        }
        writeJpeg(image, realPath, jpegQuality());
    }

    private float jpegQuality() {
        return switch (pictureQuality) {
            case QUALITY_NORMAL -> 0.78f;
            case QUALITY_SUPERFINE -> 0.96f;
            default -> 0.88f;
        };
    }

    private static void writeJpeg(BufferedImage image, Path path, float quality) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.hasNext() ? writers.next() : null;
        if (writer == null) {
            throw new IOException("JPEG writer is not available.");
        }
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameters.setCompressionQuality(Math.max(0f, Math.min(1f, quality)));
            }
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
    }

    private static void validateSizeId(int sizeId) {
        if (sizeId < 0 || sizeId >= AVAILABLE_SIZES.length) {
            throw new IllegalArgumentException("Unsupported camera size index: " + sizeId);
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Camera connection is closed.");
        }
    }
}
