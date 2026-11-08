package com.saas.procurement.media;

import java.util.Comparator;
import java.util.List;

public record MediaDto(Long id, String url, FileType type, String originalName) {

	public static MediaDto from(MediaFile m) {
		return new MediaDto(m.getId(), m.getFileUrl(), m.getFileType(), m.getOriginalName());
	}

	public static List<MediaDto> list(List<MediaFile> files) {
		return files == null ? List.of()
				: files.stream().sorted(Comparator.comparing(MediaFile::getId)).map(MediaDto::from).toList();
	}

	/** First image URL, used as the thumbnail on cards. */
	public static String firstImage(List<MediaFile> files) {
		return files == null ? null
				: files.stream().filter(f -> f.getFileType() == FileType.IMAGE)
						.min(Comparator.comparing(MediaFile::getId))
						.map(MediaFile::getFileUrl).orElse(null);
	}

}
