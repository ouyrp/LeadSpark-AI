package com.leadspark.common.api;

import java.util.List;

public record PageResult<T>(List<T> items, long page, long pageSize, long total) {
}
