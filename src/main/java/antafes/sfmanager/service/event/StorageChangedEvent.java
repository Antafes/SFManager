package antafes.sfmanager.service.event;

import antafes.sfmanager.entity.Factory;

public record StorageChangedEvent(Factory factory)
{
}
