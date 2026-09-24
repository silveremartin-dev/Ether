package org.ether.society.persistence;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Iterator;

/**
 * Pure Java Animated GIF Sequence Writer using standard ImageIO.
 * Decoupled from external binaries (ffmpeg) to guarantee zero-dependency video animation export.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class GifSequenceWriter implements AutoCloseable {

    private final ImageWriter gifWriter;
    private final ImageWriteParam imageWriteParam;
    private final IIOMetadata imageMetaData;

    public GifSequenceWriter(ImageOutputStream outputStream, int imageType, int timeBetweenFramesMS, boolean loopContinuously) throws IOException {
        gifWriter = getWriter();
        imageWriteParam = gifWriter.getDefaultWriteParam();
        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);

        imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

        String metaFormatName = imageMetaData.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(Math.max(1, timeBetweenFramesMS / 10)));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        if (loopContinuously) {
            IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
            IIOMetadataNode appExtensionNode = new IIOMetadataNode("ApplicationExtension");
            appExtensionNode.setAttribute("applicationID", "NETSCAPE");
            appExtensionNode.setAttribute("authenticationCode", "2.0");
            byte[] userObject = new byte[]{0x1, 0x0, 0x0}; // 0 = infinite loop
            appExtensionNode.setUserObject(userObject);
            appExtensionsNode.appendChild(appExtensionNode);
        }

        try {
            imageMetaData.setFromTree(metaFormatName, root);
        } catch (IIOInvalidTreeException e) {
            throw new IOException("Failed to configure GIF metadata tree", e);
        }

        gifWriter.setOutput(outputStream);
        gifWriter.prepareWriteSequence(null);
    }

    public void writeToSequence(RenderedImage img) throws IOException {
        gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
    }

    @Override
    public void close() throws IOException {
        try {
            gifWriter.endWriteSequence();
        } finally {
            gifWriter.dispose();
        }
    }

    private static ImageWriter getWriter() throws IIOException {
        Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
        if (!iter.hasNext()) {
            throw new IIOException("No standard GIF ImageWriter found in JVM ImageIO SPI registry.");
        }
        return iter.next();
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++) {
            if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
                return ((IIOMetadataNode) rootNode.item(i));
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return node;
    }
}
