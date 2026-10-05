package antafes.sfmanager.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Purity
{
    IMPURE("impure"),
    NORMAL("normal"),
    PURE("pure");

    private final String key;
}
