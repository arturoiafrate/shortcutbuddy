package it.arturoiafrate.shortcutbuddy.model.type;

import java.util.HashMap;
import java.util.Map;

public class BidirectionalMap<K, V> {
    private final Map<K, V> forwardMap = new HashMap<>();
    private final Map<V, K> reverseMap = new HashMap<>();

    public void put(K key, V value) {
        if (forwardMap.containsKey(key)) {
            reverseMap.remove(forwardMap.get(key));
        }
        if (reverseMap.containsKey(value)) {
            forwardMap.remove(reverseMap.get(value));
        }
        forwardMap.put(key, value);
        reverseMap.put(value, key);
    }

    public V getForward(K key) {
        return forwardMap.get(key);
    }

    public K getReverse(V value) {
        return reverseMap.get(value);
    }

    public void removeByKey(K key) {
        V value = forwardMap.remove(key);
        if (value != null) {
            reverseMap.remove(value);
        }
    }

    public void removeByValue(V value) {
        K key = reverseMap.remove(value);
        if (key != null) {
            forwardMap.remove(key);
        }
    }
}