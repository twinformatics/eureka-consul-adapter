package at.twinformatics.eureka.adapter.consul.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Contains "something" with its last changed index
 */
@Getter
@AllArgsConstructor
public class ChangeItem<T> {
    private T item;
    private long changeIndex;
}
