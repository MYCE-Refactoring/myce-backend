package com.myce.expo.service.info.impl;

import com.myce.expo.service.info.ExpoBookmarkService;
import com.myce.member.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExpoBookmarkServiceImpl implements ExpoBookmarkService {
  private FavoriteRepository favoriteRepository;

  @Override
  public boolean getCheckBookmark(Long memberId, Long expoId) {
    return favoriteRepository.existsByMember_IdAndExpo_Id(memberId, expoId);
  }
}
