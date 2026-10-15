# Advanced Expense & Budget Features Implementation Plan

Enhancing the Personal Expense & Budget Log app with comprehensive Vietnamese financial features:
1. **Category Management & Budget Limits with Warnings (50%, 80%, 100%)**: Set and edit monthly budget limits per category with visual alert badges.
2. **Visual Charts & Spending Breakdown**: Compose-based Pie Chart / Percentage breakdown showing exactly where money is spent.
3. **Transaction History & Advanced Filtering**: Search and filter transactions by type (Income vs Expense) and category.
4. **Monthly & Yearly Savings Analytics**: Dedicated statistics on savings, net savings rate, and historical monthly/yearly breakdowns.

## Proposed Changes

### 1. Data Model & Repository (`Models.kt`, `ExpenseRepository.kt`)
- Support updating category budget limits.
- Helper functions for calculating monthly and yearly savings.

### 2. ViewModel (`ExpenseViewModel.kt`)
- Add methods to update category budgets.
- Expose category spending percentages for chart rendering.
- Expose monthly and yearly summary statistics (Income, Expense, Savings, Savings Rate %).

### 3. Presentation Layer UI (`ui/screens/`)
- **DashboardScreen**: Add Custom Compose Pie/Donut Chart for category spending, enhanced budget warning indicators (Yellow at 50%, Orange at 80%, Red at 100%+).
- **AnalyticsScreen**: New screen displaying monthly and yearly savings statistics and trends.
- **TransactionsScreen**: Search bar and type/category filter tabs.
- **BudgetSettingsScreen**: Dialog or screen to customize category budget limits.
- **MainActivity**: Bottom navigation bar to switch between Dashboard, Transactions, and Analytics.

## Verification Plan

### Automated Tests
- Build verification via `gradle_build` (`app:assembleDebug`).

### Manual Verification
- Verify adding/deleting transactions updates charts and savings in real time.
- Verify changing category budget limits correctly triggers 50/80/100% warning badges.
- Verify monthly & yearly savings calculation accuracy.
