package io.collective;
import java.time.Clock;
import java.util.HashMap;

/**
 *  This cache uses a combination of a sorted double linked list and a hashmap.
 *
 *  The hashmap keys will point directly to the linked list node, a constant time complexity O(1).
 *
 *  The linked list will allow for constant time O(1) insertion and removal on an ordered list. (cache entries ordered by expiration)
 *
 *   put() will have a worst case time complexity of O(n), where n is the total number of cached entries. This is because
 *  sortedInsert will traverse the linked list to find the sorted insertion point. This sorting will allow
 *  for a more efficient purging of all expired entries. All expired entries will be at the front/left
 *  of the linked list. put() may be further optimized by opportunistically removing expired entries as it traverses the linked list
 *  in search of the sorted insertion point, but I deemed that an over-optimization in the context of this assignment.
 *
 *  isEmpty() and size() will have a time complexity of O(n) where n is the number of expired entries that will be removed.
 *  Entries that need to be purged will already be sorted to the front of the list. Purging expired entries must
 *  be done each time isEmpty() or size() is called to make sure the result is accurate.
 *
 */
public class SimpleAgedCache {

    private HashMap<Object, ExpirableEntry> hmap = new HashMap<>();
    private Clock clock;
    // left and right dummy nodes to allow for insertion and deletion when list is empty or shorter than 3
    private ExpirableEntry left = new ExpirableEntry(null, null, 0);
    private ExpirableEntry right = new ExpirableEntry(null, null, 0);

    /**
     * Constructor
     * @param clock timekeeping instance
     * @return SimpleAgedCache instance
     */
    public SimpleAgedCache(Clock clock) {
        this.clock = clock;
        this.left.next = this.right;
        this.right.prev = this.left;
    }

    /**
     * Default Constructor
     * @return SimpleAgedCache instance
     */
    public SimpleAgedCache() {
        this.clock = Clock.systemUTC();
        this.left.next = this.right;
        this.right.prev = this.left;
    }

    /**
     * Adds ExpirableEntry to both the hashmap and the linked list
     * @param entry the entry instance to added
     * @param l the ExpirableEntry target, the new entry is added to the right of l
     * @return void
     */
    private void insert(ExpirableEntry entry, ExpirableEntry l) {
        ExpirableEntry r = l.next;
        entry.prev = l;
        entry.next = r;
        r.prev = entry;
        l.next = entry;
        this.hmap.put(entry.key, entry);
    }

    /**
     * Removes ExpirableEntry from both the hashmap and the linked list
     * @param entry the entry instance to removed
     * @return void
     */
    private void remove(ExpirableEntry entry) {
        ExpirableEntry l = entry.prev;
        ExpirableEntry r = entry.next;
        l.next = r;
        r.prev = l;
        this.hmap.remove(entry.key);
    }

    /**
     * Finds the sorted target location and inserts an ExpirableEntry
     * @param entry the entry instance to added
     * @return void
     */
    private void insertSorted(ExpirableEntry entry) {
        ExpirableEntry l = this.left;
        while (l.expirationTime < entry.expirationTime && l.next != this.right) {
            l = l.next;
        }
        this.insert(entry, l);
    }

    /**
     * Finds all expired ExpireableEntries and removes them, this must be called before
     * isEmpty() or size() to ensure an accurate result.
     * @return void
     */
    private void removeAllExpired() {
        ExpirableEntry current = this.left.next;
        long now = clock.millis();

        while (current != this.right && current.expirationTime < now) {
            ExpirableEntry next = current.next;
            this.remove(current);
            current = next;
        }
    }

    /**
     * Creates an ExpirableEntry and inserts into the cache
     * @param key the cache key value for quick hashmap access
     * @param value the cache value
     * @param retentionInMillis the time from now() that this entry expires
     * @return void
     */
    public void put(Object key, Object value, int retentionInMillis) {
        long expirationTime = clock.millis() + (long) retentionInMillis;
        ExpirableEntry entry = new ExpirableEntry(key ,value, expirationTime);
        this.insertSorted(entry);
    }

    /**
     * Checks if the cache is currently empty
     * @return boolean
     */
    public boolean isEmpty() {
        return this.size() == 0;
    }

    /**
     * Gets the current unexpired ExpirableEntry count in cache
     * @return int
     */
    public int size() {
        this.removeAllExpired();
        return this.hmap.size();
    }

    /**
     * Returns the cache value for a valid, unexpired entry
     * @return cache entry value or null
     */
    public Object get(Object key) {
        ExpirableEntry entry = this.hmap.get(key);
        if (entry == null || entry.expirationTime < this.clock.millis()) {
            return null;
        }
        return entry.value;
    }

    /**
     * A linked list node and hashmap value for cache access
     */
    private class ExpirableEntry {
        public ExpirableEntry next = null;
        public ExpirableEntry prev = null;

        private Object key;
        private Object value;
        private long expirationTime;

        public ExpirableEntry(Object key, Object value, long expirationTime) {
            this.key = key;
            this.value = value;
            this.expirationTime = expirationTime;
        }
    }
}