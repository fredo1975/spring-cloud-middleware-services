package specifications.filter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;


public class PageRequestBuilder {
	private PageRequestBuilder() {
		
	}

	public static PageRequest getPageRequest(Integer pageSize, Integer pageNumber, String sortingCriteria) {
		var sortingFileds = new LinkedHashSet<>(
				Arrays.asList(StringUtils.split(StringUtils.defaultIfEmpty(sortingCriteria, ""), ",")));

		var sortingOrders = sortingFileds.stream().map(PageRequestBuilder::getOrder)
				.collect(Collectors.toList());

		var sort = sortingOrders.isEmpty() ? null : Sort.by(sortingOrders);

		if(sort != null) {
			return PageRequest.of(ObjectUtils.defaultIfNull(pageNumber, 1) - 1, ObjectUtils.defaultIfNull(pageSize, 20),sort);
		} else {
			return PageRequest.of(ObjectUtils.defaultIfNull(pageNumber, 1) - 1, ObjectUtils.defaultIfNull(pageSize, 20));
		}
	}

	private static Order getOrder(String value) {
		if (StringUtils.startsWith(value, "-")) {
			return new Order(Direction.DESC, StringUtils.substringAfter(value, "-"));
		} else if (StringUtils.startsWith(value, "+")) {
			return new Order(Direction.ASC, StringUtils.substringAfter(value, "+"));
		} else {
			return new Order(Direction.ASC, StringUtils.trim(value));
		}
	}
}
