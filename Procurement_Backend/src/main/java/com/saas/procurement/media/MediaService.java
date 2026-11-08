package com.saas.procurement.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.saas.procurement.common.ApiException;
import com.saas.procurement.security.AuthUser;
import com.saas.procurement.user.User;
import com.saas.procurement.user.UserRepository;

/**
 * Saves uploaded photos/videos to the local "uploads" folder and records them in media_files.
 * Later this can be swapped for cloud storage (S3) without changing the rest of the app.
 */
@Service
public class MediaService {

	private static final Logger log = LoggerFactory.getLogger(MediaService.class);

	private static final Map<String, String> IMAGE_TYPES = Map.of(
			"image/jpeg", "jpg", "image/png", "png", "image/webp", "webp", "image/gif", "gif");
	private static final Map<String, String> VIDEO_TYPES = Map.of(
			"video/mp4", "mp4", "video/webm", "webm", "video/quicktime", "mov");

	private final Path uploadDir;
	private final long maxImageBytes;
	private final long maxVideoBytes;
	private final MediaFileRepository repository;
	private final UserRepository userRepository;

	public MediaService(@Value("${app.upload-dir}") String uploadDir, @Value("${app.max-image-mb}") long maxImageMb,
			@Value("${app.max-video-mb}") long maxVideoMb, MediaFileRepository repository,
			UserRepository userRepository) {
		this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
		this.maxImageBytes = maxImageMb * 1024 * 1024;
		this.maxVideoBytes = maxVideoMb * 1024 * 1024;
		this.repository = repository;
		this.userRepository = userRepository;
	}

	/**
	 * Stores the files and links each one to its parent with {@code linker}
	 * (e.g. {@code m -> m.setProduct(product)}).
	 */
	@Transactional
	public List<MediaFile> saveAll(List<MultipartFile> files, AuthUser me, Consumer<MediaFile> linker) {
		if (files == null || files.isEmpty()) {
			throw ApiException.badRequest("Choose at least one file.");
		}
		User uploader = userRepository.getReferenceById(me.id());
		List<MediaFile> saved = new ArrayList<>();
		for (MultipartFile file : files) {
			MediaFile media = store(file);
			media.setUploadedBy(uploader);
			linker.accept(media);
			saved.add(repository.save(media));
		}
		return saved;
	}

	/** Owner of the parent item or an admin can delete. */
	@Transactional
	public void delete(Long mediaId, AuthUser me) {
		MediaFile media = repository.findById(mediaId).orElseThrow(() -> ApiException.notFound("File"));
		Long uploaderId = media.getUploadedBy() == null ? null : media.getUploadedBy().getId();
		if (!me.isAdmin() && !me.id().equals(uploaderId)) {
			throw ApiException.forbidden();
		}
		repository.delete(media);
		deleteFromDisk(media.getFileUrl());
	}

	private MediaFile store(MultipartFile file) {
		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		FileType type;
		String ext;
		if (IMAGE_TYPES.containsKey(contentType)) {
			type = FileType.IMAGE;
			ext = IMAGE_TYPES.get(contentType);
			if (file.getSize() > maxImageBytes) {
				throw ApiException.badRequest("Photo is too large (max " + maxImageBytes / 1024 / 1024 + " MB).");
			}
		}
		else if (VIDEO_TYPES.containsKey(contentType)) {
			type = FileType.VIDEO;
			ext = VIDEO_TYPES.get(contentType);
			if (file.getSize() > maxVideoBytes) {
				throw ApiException.badRequest("Video is too large (max " + maxVideoBytes / 1024 / 1024 + " MB).");
			}
		}
		else {
			throw ApiException.badRequest("Only JPG, PNG, WEBP, GIF photos and MP4, WEBM, MOV videos are allowed.");
		}

		LocalDate today = LocalDate.now();
		String folder = today.getYear() + "/" + String.format("%02d", today.getMonthValue());
		String fileName = UUID.randomUUID() + "." + ext;
		try {
			Path dir = uploadDir.resolve(folder);
			Files.createDirectories(dir);
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException e) {
			throw new IllegalStateException("Could not save uploaded file", e);
		}

		MediaFile media = new MediaFile();
		media.setFileUrl("/uploads/" + folder + "/" + fileName);
		media.setFileType(type);
		media.setOriginalName(file.getOriginalFilename());
		media.setSizeBytes(file.getSize());
		return media;
	}

	public void deleteFromDisk(String fileUrl) {
		if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
			return;
		}
		Path path = uploadDir.resolve(fileUrl.substring("/uploads/".length())).normalize();
		if (!path.startsWith(uploadDir)) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException e) {
			log.warn("Could not delete file {}", path);
		}
	}

}
