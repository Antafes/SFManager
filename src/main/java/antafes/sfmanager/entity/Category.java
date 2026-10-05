package antafes.sfmanager.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category
{
    SPECIAL("special"),
    PRODUCTION("production"),
    POWER("power"),
    LOGISTICS("logistics"),
    TRANSPORTATION("transportation"),
    ORGANIZATION("organization");

    private final String name;
}
