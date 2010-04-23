/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.hawtdb.internal.page;

import java.nio.ByteBuffer;

import org.fusesource.hawtdb.api.EncoderDecoder;
import org.fusesource.hawtdb.api.PageFile;
import org.fusesource.hawtdb.internal.io.MemoryMappedFile;
import org.fusesource.hawtbuf.Buffer;


/**
 * Provides a {@link PageFile} interface to a {@link MemoryMappedFile}. 
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class HawtPageFile implements PageFile {
    
    private final SimpleAllocator allocator;
    private final short pageSize;
    private final int headerSize;
    private final MemoryMappedFile file;
    
    
    public HawtPageFile(MemoryMappedFile file, short pageSize, int headerSize, int maxPages) {
        this.file = file;
        this.allocator = new SimpleAllocator(maxPages);
        this.pageSize = pageSize;
        this.headerSize = headerSize;
    }
    
    ///////////////////////////////////////////////////////////////////
    //
    // Paged interface implementation.
    //
    ///////////////////////////////////////////////////////////////////
    public SimpleAllocator allocator() {
        return allocator;
    }

    public int alloc() {
        return allocator().alloc(1);
    }

    public void free(int page) {
        allocator().free(page, 1);
    }

    public void read(int pageId, Buffer buffer) {
		file.read(offset(pageId), buffer);
	}

	public void write(int pageId, Buffer buffer) {
		file.write(offset(pageId), buffer);
	}
	
	public ByteBuffer slice(SliceType type, int pageId, int size) {
        assert size > 0;
        return file.slice(type==SliceType.READ, offset(pageId), pageSize*size);
    }

    public void unslice(ByteBuffer buffer) {
        file.unslice(buffer);
    }

	
    public int getPageSize() {
        return pageSize;
    }
    
    public int pages(int length) {
        assert length >= 0;
        return ((length-1)/pageSize)+1;
    }

    public void flush() {
        file.sync();
    }

    public <T> T get(EncoderDecoder<T> encoderDecoder, int page) {
        return encoderDecoder.load(this, page);
    }

    public <T> void put(EncoderDecoder<T> encoderDecoder, int page, T value) {
        encoderDecoder.store(this, page, value);
    }

    public <T> void clear(EncoderDecoder<T> encoderDecoder, int page) {
        encoderDecoder.remove(this, page);
    }
    
    ///////////////////////////////////////////////////////////////////
    //
    // PageFile public methods.
    //
    ///////////////////////////////////////////////////////////////////

    /* (non-Javadoc)
     * @see org.fusesource.hawtdb.internal.page.PageFile#write(int, java.nio.ByteBuffer)
     */
    public void write(int pageId, ByteBuffer buffer) {
        file.write(offset(pageId), buffer);
    }

    public long offset(long pageId) {
        assert pageId >= 0;
        return headerSize+(pageId*pageSize);
    }
    
    public int getHeaderSize() {
        return headerSize;
    }

    public MemoryMappedFile getFile() {
        return file;
    }

    @Override
    public String toString() {
        return "{ header size: "+headerSize+", page size: "+pageSize+", allocator: "+allocator+" }";
    }

}
