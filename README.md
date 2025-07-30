# AI Expense Tracker

An intelligent expense tracking Android application powered by Google's Gemini AI API.

## Features

- **Smart Expense Tracking**: Add, edit, and categorize your expenses and income
- **AI-Powered Insights**: Chat with Gemini AI to get personalized financial advice and insights
- **Financial Overview**: View your total income, expenses, and balance at a glance
- **Category Management**: Organize expenses by categories like Food, Transportation, Entertainment, etc.
- **Real-time Analytics**: Get instant analysis of your spending patterns
- **Modern UI**: Clean, Material Design 3 interface

## Setup Instructions

### Prerequisites

1. Android Studio (latest version)
2. Android SDK with minimum API level 24
3. Google Gemini API key

### Getting Started

1. **Clone or download the project**
   ```
   Copy the project to your desired location
   ```

2. **Get Gemini API Key**
   - Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
   - Create a new API key
   - Copy the API key

3. **Configure API Key**
   - Open `app/src/main/java/com/expensetracker/ai/utils/Constants.kt`
   - Replace `YOUR_GEMINI_API_KEY_HERE` with your actual API key:
   ```kotlin
   const val GEMINI_API_KEY = "your_actual_api_key_here"
   ```

4. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project folder and select it

5. **Build and Run**
   - Let Android Studio sync the project
   - Connect an Android device or start an emulator
   - Click "Run" button or press Ctrl+R

## Project Structure

```
app/
├── src/main/java/com/expensetracker/ai/
│   ├── data/
│   │   ├── database/          # Room database components
│   │   ├── model/            # Data models
│   │   └── repository/       # Repository pattern implementation
│   ├── network/              # Retrofit API service
│   ├── ui/
│   │   ├── adapter/          # RecyclerView adapters
│   │   ├── viewmodel/        # MVVM ViewModels
│   │   ├── MainActivity.kt   # Main activity
│   │   ├── AddExpenseActivity.kt  # Add expense screen
│   │   └── ChatActivity.kt   # AI chat interface
│   ├── utils/                # Utility classes
│   └── ExpenseTrackerApplication.kt
└── src/main/res/             # UI layouts and resources
```

## Key Components

### Database
- **Room Database**: Local storage for expenses
- **Expense Entity**: Stores amount, category, description, date, and type (income/expense)
- **DAO**: Data access operations with LiveData/Flow for reactive UI

### AI Integration
- **Gemini API**: Integrated using Retrofit
- **Context-Aware Chat**: AI responses include current financial data
- **Smart Insights**: Get advice on spending patterns, budgeting, and financial goals

### UI Components
- **Material Design 3**: Modern, clean interface
- **Financial Dashboard**: Quick overview of financial status
- **Expense List**: RecyclerView with smooth animations
- **Chat Interface**: Real-time conversation with AI assistant

## Usage

1. **Adding Expenses**
   - Tap the "+" floating action button
   - Fill in amount, category, description
   - Choose between income or expense
   - Select date and save

2. **AI Chat**
   - Tap the chat icon (mini FAB) or menu item
   - Ask questions about your spending
   - Get personalized financial advice
   - Analyze spending patterns

3. **View Analytics**
   - Main screen shows financial summary
   - Income, expenses, and balance display
   - Recent transactions list

## Example AI Conversations

- "How much did I spend on food this month?"
- "What's my biggest expense category?"
- "Give me tips to save money"
- "Am I spending too much on entertainment?"
- "Help me create a budget"

## Technical Features

- **MVVM Architecture**: Clean separation of concerns
- **Repository Pattern**: Centralized data management
- **Coroutines**: Asynchronous operations
- **LiveData/Flow**: Reactive UI updates
- **Material Design**: Modern Android UI guidelines
- **Room Database**: Efficient local storage

## Requirements

- Android 7.0 (API level 24) or higher
- Internet connection for AI features
- Valid Gemini API key

## License

This project is for educational and personal use. Make sure to follow Google's Gemini API terms of service.

## Troubleshooting

### Common Issues

1. **API Key Error**
   - Ensure you've replaced the placeholder with your actual API key
   - Verify the API key is valid and has proper permissions

2. **Build Errors**
   - Clean and rebuild the project
   - Check that all dependencies are properly downloaded

3. **Network Issues**
   - Ensure device has internet connection
   - Check if API endpoints are accessible

### Support

For issues or questions:
1. Check the troubleshooting section
2. Review the setup instructions
3. Verify your API key configuration

## Future Enhancements

- Export data to CSV/PDF
- Budget setting and alerts
- Multiple currency support
- Data backup and sync
- Expense photos and receipts
- Advanced analytics and charts
