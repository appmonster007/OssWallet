1. Add timeline filtering to al cards [X]
2. Update the transaction creation screen [X]
3. Add view for transactions
4. Add filters, filter creations and management view 
5. Add nav bar filters in transactionsScreen 
6. When you click on cell in aggrate currency balances or cashflow summary it should open transaction view with that filter
7. Add view for analytics screen 
8. Bring calc numpad when clicked on the amount space, add tick to close the numpad
9.

[x] when clicking the edit category, open the sheet full page, create reusable element out of the sheet

[x] in the settings group different settings under preference, organization, data & security, here instead of showing settings as individual list, create a reusable card view for each grouping that shows the group name and then the list of settings similar to a segmented view as a group

@AccountDetailScreen.kt review the design for transactions list and redesign the page for material 3 expressive,

[ ] in @AccountDetailScreen.kt's settings add show planned payment to show toggle switch for showing payments in future from current date and time, add toggle switch to show the category as transactionitem's header or to show the note as header,
move the transaction's recorded date to right bottom below the opening and closing balance, the transaction list section header to show "today" keyword instead of date if the date of transaction is the current day,
add sticky on top during scroll section header to indicate the week of the year currently visible on the top of the lists items, with closing balance of the week and total aggregate of expense and income sum for that week only
dont show date time headers in the list show date & time on the items itself

[x] if labels exists for transaction show the labels badges at the bottom of the transaction item remove the rounded corner backgroud from each item, remove the padding or any space between the transaction items and add top and down border for line separation

@TransactionEntryScreen there is too much wasted space in between two rows of label badges, reduce that space

in the reusable component make the accounts selection field , category selection field, currencies selection field, payment method selection field into segmented buttons, on each button press it shows the selection dialog and if there is any selection done then update the value in segmented button with count of selection and highlight enabled,
and in the segmented button group use icons instead of text to represent each field to fit in single row

