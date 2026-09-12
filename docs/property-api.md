# Property API Documentation

## List All Properties

**Endpoint:** `GET /api/v1/properties/all`

Retrieves a list of all properties in the system. This endpoint supports intelligent location-based filtering with enhanced full address matching capabilities.

### Query Parameters

| Parameter | Type   | Description                                                             |
|-----------|--------|-------------------------------------------------------------------------|
| location  | string | Smart location search across all address fields. The system intelligently detects full addresses, postal codes, cities, streets, or other location identifiers and uses advanced matching algorithms to find relevant properties. Results are ranked by relevance when searching with full addresses. |

### Enhanced Full Address Search

When you search with a full or partial address (e.g., "123 Main Street, London"), the system:

1. Breaks down the address into components
2. Searches across all address fields for each component
3. Ranks results based on how well they match the full query
4. Prioritizes properties with more matching components
5. Gives higher priority to house numbers and postal codes

### Example Requests

```
# Get all properties
GET /api/v1/properties/all

# Search with a full address
GET /api/v1/properties/all?location=123 Main Street, London W1 1AA

# Filter properties by postal code
GET /api/v1/properties/all?location=W1 1AA

# Filter properties by city
GET /api/v1/properties/all?location=London

# Search properties with a street name
GET /api/v1/properties/all?location=Main Street
```

### Response

```json
{
  "status": "success",
  "message": "Properties fetched successfully",
  "data": [
    {
      "id": "uuid",
      "name": "Property Name",
      "size": 1500,
      "price": 250000,
      "type": "HOUSE",
      "status": "SELL",
      "landlord": {
        "id": "uuid",
        "user": {
          "id": "uuid",
          "first_name": "John",
          "last_name": "Doe",
          "email": "john@example.com"
        }
      },
      "property_address": [
        {
          "id": "uuid",
          "current": true,
          "address": {
            "id": "uuid",
            "house_number": "123",
            "building_name": "Building Name",
            "street": "Main Street",
            "town": "Town Name",
            "city": "City Name",
            "postal_code": "12345",
            "description": "Property description",
            "notes": "Property notes"
          }
        }
      ],
      "property_image": [
        {
          "id": "uuid",
          "image_type": "property",
          "description": "Property image",
          "notes": "Property image",
          "file": {
            "id": "uuid",
            "name": "image.jpg",
            "path": "/storage/image.jpg",
            "type": "image/jpeg",
            "size": 123456
          }
        }
      ]
    }
  ]
}
```
