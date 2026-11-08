package com.saas.procurement.category;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.saas.procurement.common.ApiException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryRepository repository;

	public record CategoryDto(Long id, String name) {
		public static CategoryDto from(Category c) {
			return c == null ? null : new CategoryDto(c.getId(), c.getName());
		}
	}

	public record CategoryRequest(@NotBlank @Size(max = 100) String name) {
	}

	/** Public: used by the landing page and by every form with a category dropdown. */
	@GetMapping("/public/categories")
	public List<CategoryDto> list() {
		return repository.findAllByOrderByNameAsc().stream().map(CategoryDto::from).toList();
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/admin/categories")
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryDto create(@Valid @RequestBody CategoryRequest req) {
		String name = req.name().trim();
		if (repository.existsByNameIgnoreCase(name)) {
			throw ApiException.conflict("Category '" + name + "' already exists.");
		}
		return CategoryDto.from(repository.save(new Category(name)));
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/admin/categories/{id}")
	@Transactional
	public CategoryDto rename(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
		Category c = repository.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
		c.setName(req.name().trim());
		return CategoryDto.from(c);
	}

	/** Products/requests in this category keep existing with no category. */
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/admin/categories/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		repository.deleteById(id);
	}

}
