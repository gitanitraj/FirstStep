package org.firststep.backend.shared.dto;

import java.util.List;

public class PageResponse<T> {
    public List<T> content;
    public int page;
    public int size;
    public long totalElements;
}
