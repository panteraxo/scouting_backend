package com.scouting.scouting.media.repository;

import com.scouting.scouting.media.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {
}
