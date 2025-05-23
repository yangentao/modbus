/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2016 Glenn Lane
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kerbaya.ieee754lib;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public final class BitUtils
{
	private BitUtils() {}
	
	public static BitSource wrapSource(ReadableByteChannel source)
	{
		return new ChannelBitSource(source);
	}
	
	public static BitSource wrapSource(InputStream source)
	{
		return wrapSource(Channels.newChannel(source));
	}
	
	public static BitSource wrapSource(ByteBuffer source)
	{
		return new BufferBitSource(source);
	}
	
	public static BitSource wrapSource(byte[] source)
	{
		return wrapSource(ByteBuffer.wrap(source));
	}
	
	public static BitSource wrapSource(byte[] source, int offset, int length)
	{
		return wrapSource(ByteBuffer.wrap(source, offset, length));
	}
	
	public static FlushableBitSink wrapSink(WritableByteChannel dest)
	{
		return new ChannelBitSink(dest);
	}
	
	public static FlushableBitSink wrapSink(OutputStream dest)
	{
		return wrapSink(Channels.newChannel(dest));
	}
	
	public static BitSink wrapSink(ByteBuffer dest)
	{
		return new BufferBitSink(dest);
	}
	
	public static BitSink wrapSink(byte[] dest)
	{
		return wrapSink(ByteBuffer.wrap(dest));
	}
	
	public static BitSink wrapSink(byte[] dest, int offset, int length)
	{
		return wrapSink(ByteBuffer.wrap(dest, offset, length));
	}
}
