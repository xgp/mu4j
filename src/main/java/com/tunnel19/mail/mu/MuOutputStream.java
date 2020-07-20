package com.tunnel19.mail.mu;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * Extended from zt-exec's LogOutputStream.
 */
public abstract class MuOutputStream extends OutputStream {

    /** Initial buffer size. */
    private static final int INTIAL_SIZE = 132;

    /** Carriage return */
    private static final int CR = 0x0d;

    /** Linefeed */
    private static final int LF = 0x0a;

    /** the internal buffer */
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(INTIAL_SIZE);

    byte lastReceivedByte;

    boolean inSexpr = false;
    boolean inLength = false;
  boolean prompt = false;
    long length;
    
    /**
     * Write the data to the buffer and flush the buffer, if a line separator is
     * detected.
     *
     * @param cc data to log (byte).
     * @see java.io.OutputStream#write(int)
     */
    public void write(final int cc) throws IOException {
      final byte c = (byte) cc;
	if (inSexpr) {
	    buffer.write(cc);
	    if (buffer.size() == length) {
		processBuffer();
		inSexpr = false;
	    }
	    //	} else if (c == '\376') {
	} else if (cc == -2) {
	    System.out.println("in the length header");
	    inLength = true;
	    //	} else if (inLength && c == '\377') {
	} else if (inLength && cc == -1) {
	    System.out.println("leaving the length header");
	    inLength = false;
            try {
              byte[] b = buffer.toByteArray();
              System.out.println("length buffer: "+new String(b, StandardCharsets.UTF_8));
              length = new BigInteger(new String(b,
                                                 StandardCharsets.UTF_8), 16).longValue();
              System.out.println("length: "+length);
            } catch (Exception e) {
              e.printStackTrace();
            }
	    buffer.reset();
	    if (length > 0) inSexpr = true;
	} else if ((c == '\n') || (c == '\r')) {
	    // new line is started in case of
	    // - CR (regardless of previous character)
	    // - LF if previous character was not CR and not LF
	    if (c == '\r' || (c == '\n' && (lastReceivedByte != '\r' && lastReceivedByte != '\n'))) {
		processBuffer();
	    }
        } else if (c == '>' && buffer.size() == 5) {
          buffer.write(cc);
          // could be a prompt. mark the boolean if it is
          if (";; mu>".equals(new String(buffer.toByteArray(), StandardCharsets.UTF_8))) {
            prompt = true;
          }
	} else if (prompt && c == ' ') {
          prompt = false;
          processBuffer();
        } else {
	    buffer.write(cc);
	}
	lastReceivedByte = c;
    }

    /**
     * Flush this log stream.
     *
     * @see java.io.OutputStream#flush()
     */
    public void flush() {
	if (buffer.size() > 0) {
	    processBuffer();
	}
    }

    /**
     * Writes all remaining data from the buffer.
     *
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException {
	if (buffer.size() > 0) {
	    processBuffer();
	}
	super.close();
    }

    /**
     * Write a block of characters to the output stream
     *
     * @param b the array containing the data
     * @param off the offset into the array where data starts
     * @param len the length of block
     * @throws java.io.IOException if the data cannot be written into the stream.
     * @see java.io.OutputStream#write(byte[], int, int)
    public void write(final byte[] b, final int off, final int len)
	throws IOException {
	// find the line breaks and pass other chars through in blocks
	int offset = off;
	int blockStartOffset = offset;
	int remaining = len;
	while (remaining > 0) {
	    while (remaining > 0 && b[offset] != LF && b[offset] != CR) {
		offset++;
		remaining--;
	    }
	    // either end of buffer or a line separator char
	    int blockLength = offset - blockStartOffset;
	    if (blockLength > 0) {
		buffer.write(b, blockStartOffset, blockLength);
		lastReceivedByte = 0;
	    }
	    while (remaining > 0 && (b[offset] == LF || b[offset] == CR)) {
		write(b[offset]);
		offset++;
		remaining--;
	    }
	    blockStartOffset = offset;
	}
    }
     */

    /**
     * Converts the buffer to a string and sends it to <code>processLine</code>.
     */
    protected void processBuffer() {
	if (inSexpr) {
 	    if (buffer.size() != length) return;
	    processSexpr(buffer.toString());
	} else {
	    processLine(buffer.toString());
	}
	buffer.reset();
    }

    /**
     * Logs a line to the log system of the user.
     *
     * @param line
     *            the line to log.
     */
    protected abstract void processLine(String line);

    protected abstract void processSexpr(String sexpr);    
}
