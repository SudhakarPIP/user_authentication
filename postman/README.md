# Postman Collection

## Import Instructions

1. Open Postman
2. Click **Import** button
3. Select `User_Authentication_API.postman_collection.json`
4. The collection will be imported with all endpoints

## Environment Variables

Set up the following environment variables in Postman:

- `base_url`: `http://localhost:8080` (for local) or your AWS endpoint
- `verification_token`: Set this after signup (get from database or email)
- `jwt_token`: Automatically set after successful login

## Collection Structure

### 1. Signup Endpoints
- **Signup - Success**: Create a new user account
- **Signup - Duplicate Username**: Test duplicate username validation
- **Signup - Duplicate Email**: Test duplicate email validation
- **Signup - Validation Error (Invalid Email)**: Test email format validation
- **Signup - Validation Error (Short Password)**: Test password length validation

### 2. Email Verification Endpoints
- **Verify Email - Success**: Verify email with valid token
- **Verify Email - Invalid Token**: Test invalid token handling
- **Verify Email - Missing Token**: Test missing token parameter

### 3. Login Endpoints
- **Login - Success with Username**: Login using username
- **Login - Success with Email**: Login using email address
- **Login - Account Not Activated**: Test login before verification
- **Login - Invalid Credentials**: Test wrong password
- **Login - User Not Found**: Test non-existent user
- **Login - Missing Fields**: Test validation errors

## Usage Flow

1. **Signup**: Use "Signup - Success" to create a new user
2. **Get Token**: Query database or check email for verification token
   ```sql
   SELECT token FROM verification_tokens WHERE used = false ORDER BY created_at DESC LIMIT 1;
   ```
3. **Set Token**: Update `verification_token` variable in Postman
4. **Verify**: Use "Verify Email - Success" to activate account
5. **Login**: Use "Login - Success with Username" or "Login - Success with Email"

## Testing

All requests include automated tests that verify:
- HTTP status codes
- Response structure
- Error messages
- Token presence (for login)

Run the collection using Postman's Collection Runner for automated testing.

