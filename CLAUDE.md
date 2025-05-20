# Banking Application Code Generation Guidelines

## Project Context
This is a fullstack banking application following Spring Boot (backend) and Vue.js (frontend) architecture. The assessment emphasizes code quality (90%) and API design (10%).

## Backend (Spring Boot) Patterns
- **Package Structure**: Follow `com.bankapp.*` organization
- **Controllers**:
  - Use `@RestController` and `@RequestMapping("path")`
  - Inject services with `@Autowired`
  - Return appropriate HTTP status codes
  - Follow RESTful naming (`/users`, `/accounts`, `/transactions`)

- **Services**:
  - Implement business logic in service layer
  - Mark with `@Service` annotation
  - Handle transaction management with `@Transactional` where needed

- **Models**:
  - Use JPA annotations (`@Entity`, `@Id`, `@GeneratedValue`)
  - Implement proper relationships (`@OneToMany`, `@ManyToOne`)
  - Include appropriate validation annotations

- **Repositories**:
  - Extend `JpaRepository<Entity, IdType>`
  - Use Spring Data JPA query methods

- **Security**:
  - JWT authentication via existing `JwtUtil` and `JwtAuthenticationFilter`
  - Role-based authorization (CUSTOMER vs EMPLOYEE roles)

- **Testing**:
  - Unit tests with JUnit
  - Functional tests with Cucumber

## Frontend (Vue.js) Patterns
- **Component Structure**:
  - Views in `src/views` directory
  - Consistent naming with `*View.vue` suffix
  - Use Vue Router with routes defined in `router/index.js`

- **State Management**:
  - Use Vuex store pattern in `store/*.js`
  - Follow existing authentication flow in `store/auth.js`

- **API Integration**:
  - Consistent error handling
  - JWT token management in requests

## Key Features to Maintain
- User registration and authentication
- Account management (checking/savings)
- Customer money transfers
- Transaction history and filtering
- ATM operations (deposit/withdraw)
- Employee approval workflows

When generating code, focus on maintaining consistent patterns with existing implementations and ensuring it meets the these requirements.