package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.DatasetItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetItemRepository extends JpaRepository<DatasetItem, Long> {

  List<DatasetItem> findByDatasetIdOrderByIdAsc(Long datasetId);
}
