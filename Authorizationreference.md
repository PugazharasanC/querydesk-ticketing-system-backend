// ─────────────────────────────────────────────────────────
// AUTHORIZATION REFERENCE — how to use in every controller
// ─────────────────────────────────────────────────────────

// 1. INJECT CURRENT USER in any controller method:
//
//    @GetMapping("/me")
//    public ResponseEntity<?> getMe(@CurrentUser UserPrincipal currentUser) {
//        return ResponseEntity.ok(currentUser.getId());
//    }


// 2. ROLE-BASED ACCESS on methods:
//
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<?> adminOnly() { ... }
//
//    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
//    public ResponseEntity<?> managerAndAbove() { ... }
//
//    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
//    public ResponseEntity<?> agentAndAbove() { ... }
//
//    @PreAuthorize("hasRole('CUSTOMER')")
//    public ResponseEntity<?> customerOnly() { ... }


// 3. OWNERSHIP CHECK in services:
//
//    // Only owner or admin can access:
//    if (!SecurityUtils.isOwnerOrAdmin(ticket.getCreatedBy())) {
//        throw new UnauthorizedException("Access denied");
//    }
//
//    // Get current user anywhere in a service:
//    String currentUserId = SecurityUtils.getCurrentUserId();
//    UserPrincipal currentUser = SecurityUtils.getCurrentUser();


// 4. TICKET CONTROLLER will look like this:
//
//    @PostMapping
//    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT', 'MANAGER', 'ADMIN')")
//    public ResponseEntity<TicketResponse> createTicket(
//            @Valid @RequestBody CreateTicketRequest request,
//            @CurrentUser UserPrincipal currentUser) { ... }
//
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
//    public ResponseEntity<TicketResponse> getTicket(@PathVariable String id) { ... }
//
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> deleteTicket(@PathVariable String id) { ... }


// 5. ROLE MATRIX for this app:
//
//    Action                        CUSTOMER  AGENT  MANAGER  ADMIN
//    ─────────────────────────────────────────────────────────────
//    Create ticket                    ✅       ✅      ✅      ✅
//    View own tickets                 ✅       ✅      ✅      ✅
//    View all tickets                 ❌       ✅      ✅      ✅
//    Assign ticket to agent           ❌       ❌      ✅      ✅
//    Update ticket status             ❌       ✅      ✅      ✅
//    Escalate ticket                  ❌       ❌      ✅      ✅
//    Delete ticket                    ❌       ❌      ❌      ✅
//    Manage users                     ❌       ❌      ❌      ✅
//    Manage teams                     ❌       ❌      ✅      ✅
//    View reports/analytics           ❌       ❌      ✅      ✅