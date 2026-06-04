package dev.lucasxie.learning.common.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

	private List<T> items;

	private long total;

	private int page;

	private int size;

	private int totalPages;
}
