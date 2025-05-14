package gaiasky;

import gaiasky.util.LruCacheLong;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link LruCacheLong}.
 */
public class LruCacheLongTest {

    @Test
    public void testPutAndGet() {
        LruCacheLong<String> cache = new LruCacheLong<>(3);
        cache.put(1L, "A");
        cache.put(2L, "B");
        cache.put(3L, "C");

        assertEquals("A", cache.get(1L));
        assertEquals("B", cache.get(2L));
        assertEquals("C", cache.get(3L));
    }

    @Test
    public void testEviction() {
        LruCacheLong<String> cache = new LruCacheLong<>(2);
        cache.put(1L, "A");
        cache.put(2L, "B");
        cache.put(3L, "C");

        assertNull(cache.get(1L)); // "A" should be evicted
        assertEquals("B", cache.get(2L));
        assertEquals("C", cache.get(3L));
    }

    @Test
    public void testUpdateValue() {
        LruCacheLong<String> cache = new LruCacheLong<>(2);
        cache.put(1L, "A");
        cache.put(1L, "B");
        assertEquals("B", cache.get(1L));
    }

    @Test
    public void testRemove() {
        LruCacheLong<String> cache = new LruCacheLong<>(2);
        cache.put(1L, "A");
        cache.put(2L, "B");
        cache.remove(1L);
        assertNull(cache.get(1L));
        assertEquals("B", cache.get(2L));
    }

    @Test
    public void testClear() {
        LruCacheLong<String> cache = new LruCacheLong<>(2);
        cache.put(1L, "A");
        cache.put(2L, "B");
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get(1L));
        assertNull(cache.get(2L));
    }

    @Test
    public void testGetLeastRecentlyUsed() {
        LruCacheLong<String> cache = new LruCacheLong<>(3);
        cache.put(1L, "A");
        cache.put(2L, "B");
        cache.put(3L, "C");

        assertEquals("A", cache.getLeastRecentlyUsed());

        cache.get(1L);
        assertEquals("B", cache.getLeastRecentlyUsed());

        cache.put(4L, "D");
        assertEquals("C", cache.getLeastRecentlyUsed());
    }
}

