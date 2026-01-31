package com.myce.expo.service.info;

import com.myce.expo.dto.CategoryResponse;
import java.util.List;

public interface ExpoCategoryService {
  List<CategoryResponse> getCategories();
}
