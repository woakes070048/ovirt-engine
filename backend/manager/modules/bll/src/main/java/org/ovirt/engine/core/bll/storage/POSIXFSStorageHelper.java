package org.ovirt.engine.core.bll.storage;

import org.ovirt.engine.core.common.businessentities.StorageType;

/**
 * Storage helper for Posix FS connections
 */
public class POSIXFSStorageHelper extends BaseFsStorageHelper {

    @Override
    protected StorageType getType() {
        return StorageType.POSIXFS;
    }
}
