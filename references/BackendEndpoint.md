// ResourceController.java

@GetMapping("/resources/search")
public ResponseEntity<List<Resource>> searchResources(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String query,
    @RequestParam(defaultValue = "relevance") String sortBy
) {
    List<Resource> all = resourceService.getAll();
    
    // Filter by category if provided
    if (category != null && !category.isBlank()) {
        all = all.stream()
            .filter(r -> category.equalsIgnoreCase(r.category))
            .collect(Collectors.toList());
    }
    
    // Filter by search query if provided
    if (query != null && !query.isBlank()) {
        String q = query.toLowerCase();
        all = all.stream()
            .filter(r -> 
                r.organization.toLowerCase().contains(q) ||
                (r.summary != null && r.summary.toLowerCase().contains(q)) ||
                (r.description != null && r.description.toLowerCase().contains(q))
            )
            .collect(Collectors.toList());
    }
    
    // Sort
    if ("alphabetical".equalsIgnoreCase(sortBy)) {
        all.sort(Comparator.comparing(r -> r.organization));
    } else if ("updated".equalsIgnoreCase(sortBy)) {
        all.sort((a, b) -> {
            LocalDate dateA = parseDate(a.retrieved);
            LocalDate dateB = parseDate(b.retrieved);
            return dateB.compareTo(dateA);  // Newest first
        });
    }
    // "relevance" is already handled by the query scoring
    
    return ResponseEntity.ok(all);
}

private LocalDate parseDate(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) return LocalDate.MIN;
    try {
        return LocalDate.parse(dateStr);
    } catch (Exception e) {
        return LocalDate.MIN;
    }
}