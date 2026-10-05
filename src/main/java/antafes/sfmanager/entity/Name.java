package antafes.sfmanager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Name
{
    private String english;
    private String german;

    public String getLocalizedName(String language)
    {
        if (language == null) {
            return this.english;
        }

        String localizedName = switch (language) {
            case "English" -> this.english;
            case "German" -> this.german;
            default -> null;
        };
        return localizedName != null ? localizedName : this.english;
    }
}
