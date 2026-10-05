package antafes.sfmanager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Name
{
    private String english;
    private String german;
}
