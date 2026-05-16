package com.example.baskit.list;
import android.annotation.SuppressLint;
import android.view.WindowManager;

import com.example.baskit.categories.ItemViewPricesAdapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.example.baskit.categories.ItemViewAlertDialog.VariationsManager;
import com.example.baskit.main_components.Item.ItemInfo;
import com.example.baskit.main_components.Item.ItemVariant;
import com.example.baskit.MasterActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.baskit.online_components.APIHandler;
import com.example.baskit.Baskit;
import com.example.baskit.main_components.Item;
import com.example.baskit.main_components.Supermarket;
import com.example.baskit.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;

public class AddItemFragment extends DialogFragment
{
    Item selectedItem;

    ArrayList<String> allItemNames;
    final ArrayList<String> masterItemNames;
    final ArrayList<String> decodedItemNames = new ArrayList<>();
    final ArrayList<String> decodedItemNamesLower = new ArrayList<>();
    ChipGroup chipGroupWeights;
    ChipGroup chipGroupCompanies;
    final ArrayList<ItemInfo> currentVariations = new ArrayList<>();
    ArrayList<String> listItemNames;
    final ArrayList<String> itemSuggestions;
    final Map<String, ArrayList<String>> groups;

    final APIHandler apiHandler = APIHandler.getInstance();

    ItemViewPricesAdapter pricesAdapter;

    View fragmentView;
    TextView tvQuantity;
    ImageButton btnCancel, btnUp, btnDown;
    Button btnAddItem;
    LinearLayout infoLayout;
    ProgressBar progressBar;
    AutoCompleteTextView searchItem;
    RecyclerView recyclerSupermarkets;

    final MasterActivity activity;
    final Context context;
    final AddItemInterface addItemInterface;

    public interface AddItemInterface
    {
        void addItem(Item item) throws IOException;
    }

    public AddItemFragment(
            MasterActivity activity,
            Context context,
            Map<String, ArrayList<String>> groups,
            ArrayList<String> listItemNames,
            AddItemInterface addItemInterface,
            ArrayList<String> itemSuggestions)
    {
        this.activity = activity;
        this.context = context;
        this.groups = groups;
        this.listItemNames = listItemNames;
        this.addItemInterface = addItemInterface;
        this.itemSuggestions = itemSuggestions;

        this.masterItemNames = groups != null
                ? new ArrayList<>(groups.keySet())
                : new ArrayList<>();

        this.allItemNames = new ArrayList<>(masterItemNames);

        init();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState)
    {
        fragmentView = inflater.inflate(R.layout.fragment_add_item, container, false);

        btnCancel = fragmentView.findViewById(R.id.btn_cancel);
        btnAddItem = fragmentView.findViewById(R.id.btn_add_item);
        searchItem = fragmentView.findViewById(R.id.searchItem);
        btnUp = fragmentView.findViewById(R.id.btn_up);
        btnDown = fragmentView.findViewById(R.id.btn_down);
        tvQuantity = fragmentView.findViewById(R.id.tv_quantity);
        infoLayout = fragmentView.findViewById(R.id.lout_info);
        progressBar = fragmentView.findViewById(R.id.progressBar);
        recyclerSupermarkets = fragmentView.findViewById(R.id.recycler_supermarket);
        recyclerSupermarkets.setLayoutManager(new LinearLayoutManager(context));

        chipGroupWeights = fragmentView.findViewById(R.id.chip_group_weights);
        chipGroupCompanies = fragmentView.findViewById(R.id.chip_group_units);

        setupAutocomplete();
        setupButtons();

        return fragmentView;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // fullscreen
        if (getDialog() != null && getDialog().getWindow() != null)
        {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );

            getDialog().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            );
        }

        selectedItem = null;
        searchItem.setText("");
        infoLayout.setVisibility(View.INVISIBLE);
        tvQuantity.setText("");
        searchItem.setFocusableInTouchMode(true);
        searchItem.requestFocus();

        // Reset filter chips when reopening dialog
        if (chipGroupWeights != null)
        {
            for (int i = 0; i < chipGroupWeights.getChildCount(); i++)
            {
                Chip chip = (Chip) chipGroupWeights.getChildAt(i);
                chip.setChecked(false);
            }
        }

        if (chipGroupCompanies != null)
        {
            for (int i = 0; i < chipGroupCompanies.getChildCount(); i++)
            {
                Chip chip = (Chip) chipGroupCompanies.getChildAt(i);
                chip.setChecked(false);
            }
        }

        if (!currentVariations.isEmpty())
        {
            applyVariationFilter();
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        fragmentView = null;
        recyclerSupermarkets = null;
        pricesAdapter = null;
    }

    private void init()
    {
        if (masterItemNames != null)
        {
            allItemNames = new ArrayList<>(masterItemNames);
        }

        LinkedHashMap<String, String> unique = new LinkedHashMap<>();

        if (listItemNames != null)
        {
            ArrayList<String> decodedList = new ArrayList<>();

            for (String n : listItemNames)
            {
                String d = Baskit.decodeKey(n);
                if (!isBadItemName(d)) decodedList.add(d.trim());
            }

            listItemNames = decodedList;
        }

        if (allItemNames != null)
        {
            for (String name : allItemNames)
            {
                String decoded = Baskit.decodeKey(name);
                if (isBadItemName(decoded) || (listItemNames != null && listItemNames.contains(decoded))) continue;

                String trimmed = decoded.trim();
                String key = trimmed.toLowerCase(Locale.ROOT);

                if (key.isEmpty() || key.equals("null")) continue;

                if (!unique.containsKey(key))
                {
                    unique.put(key, trimmed);
                }
            }
        }

        this.allItemNames = new ArrayList<>(unique.values());

        decodedItemNames.clear();
        decodedItemNamesLower.clear();

        for (String name : this.allItemNames)
        {
            String decoded = Baskit.decodeKey(name);
            if (decoded == null) decoded = name;
            decoded = decoded.trim();

            decodedItemNames.add(decoded);
            decodedItemNamesLower.add(decoded.toLowerCase(Locale.ROOT));
        }
    }

    private void setupAutocomplete()
    {
        final ArrayList<String> filteredResults = new ArrayList<>();
        final ArrayList<Boolean> filteredIsSuggestion = new ArrayList<>();
        final ArrayList<String> suggestionKeywords = new ArrayList<>();

        if (itemSuggestions != null)
        {
            for (String s : itemSuggestions)
            {
                String dec = Baskit.decodeKey(s);

                if (dec != null)
                {
                    String trimmed = dec.trim();
                    if (!trimmed.isEmpty()) suggestionKeywords.add(trimmed.toLowerCase(Locale.ROOT));
                }
            }
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.add_item_dropdown_item, filteredResults) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = activity.getLayoutInflater()
                            .inflate(R.layout.add_item_dropdown_item, parent, false);
                }

                TextView tvName = convertView.findViewById(R.id.tvItemName);
                ImageView ivStar = convertView.findViewById(R.id.ivStar);

                if (position < 0 || position >= filteredResults.size()) {
                    tvName.setText("");
                    if (ivStar != null) ivStar.setVisibility(View.GONE);
                    return convertView;
                }

                String decoded = Baskit.decodeKey(filteredResults.get(position));
                boolean isSuggestion = false;

                if (position < filteredIsSuggestion.size()) {
                    isSuggestion = filteredIsSuggestion.get(position);
                }

                if (ivStar != null) {
                    ivStar.setVisibility(isSuggestion ? View.VISIBLE : View.GONE);
                }

                CharSequence userInput = searchItem.getText();

                if (userInput != null && userInput.length() > 0) {
                    String[] inputWords = userInput.toString().trim().split("\\s+");
                    String decodedLower = decoded.toLowerCase(Locale.ROOT);
                    ArrayList<int[]> matchRanges = new ArrayList<>();

                    for (String word : inputWords) {
                        if (!word.isEmpty()) {
                            String wordLower = word.toLowerCase(Locale.ROOT);
                            int startIdx = 0;

                            while (startIdx < decoded.length()) {
                                int found = decodedLower.indexOf(wordLower, startIdx);
                                if (found == -1) break;

                                matchRanges.add(new int[]{found, found + word.length()});
                                startIdx = found + word.length();
                            }
                        }
                    }

                    if (!matchRanges.isEmpty()) {
                        matchRanges.sort(Comparator.comparingInt(a -> a[0]));

                        ArrayList<int[]> merged = new ArrayList<>();
                        int[] prev = matchRanges.get(0);

                        for (int i = 1; i < matchRanges.size(); i++) {
                            int[] curr = matchRanges.get(i);

                            if (curr[0] <= prev[1]) {
                                prev[1] = Math.max(prev[1], curr[1]);
                            } else {
                                merged.add(prev);
                                prev = curr;
                            }
                        }

                        merged.add(prev);
                        matchRanges = merged;
                    }

                    android.text.SpannableString spannable = new android.text.SpannableString(decoded);
                    int highlightColor = Baskit.getAppColor(context, android.R.attr.colorPrimary);

                    for (int[] range : matchRanges) {
                        int start = Math.max(0, Math.min(decoded.length(), range[0]));
                        int end = Math.max(0, Math.min(decoded.length(), range[1]));

                        if (start < end) {
                            spannable.setSpan(new android.text.style.ForegroundColorSpan(highlightColor), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            spannable.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }

                    tvName.setText(spannable, TextView.BufferType.SPANNABLE);
                } else {
                    tvName.setText(decoded);
                }
                return convertView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return getView(position, convertView, parent);
            }

            @NonNull
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = filteredResults;
                        results.count = filteredResults.size();

                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                    }
                };
            }
        };

        searchItem.setAdapter(adapter);
        searchItem.setThreshold(1);
        searchItem.setOnClickListener(v -> searchItem.showDropDown());
        searchItem.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);

        searchItem.setOnEditorActionListener((v, actionId, event) ->
        {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            {
                searchItem.clearFocus();
                hideKeyboard();
                return true;
            }
            return false;
        });

        searchItem.setDropDownAnchor(searchItem.getId());
        searchItem.setDropDownVerticalOffset(0);

        searchItem.post(() ->
        {
            if (fragmentView == null) return;

            int fragmentHeight = fragmentView.getHeight();
            int searchBottom = searchItem.getBottom();
            int maxHeightInsideFragment = fragmentHeight - searchBottom;

            int itemHeightPx = (int)(48 * fragmentView.getResources().getDisplayMetrics().density);
            int itemCount = Math.max(1, filteredResults.size());
            int desiredHeight = itemHeightPx * itemCount;

            if (maxHeightInsideFragment > 0)
            {
                searchItem.setDropDownHeight(Math.min(desiredHeight, maxHeightInsideFragment));
            }
        });

        searchItem.addTextChangedListener(new android.text.TextWatcher()
        {
            private Runnable filterRunnable = null;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable str) {
                if (filterRunnable != null) searchItem.removeCallbacks(filterRunnable);

                filterRunnable = () -> {
                    String input = str == null ? "" : str.toString();
                    ArrayList<String> inputWords = new ArrayList<>();
                    ArrayList<Boolean> wordCompleted = new ArrayList<>();

                    if (!input.isEmpty())
                    {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\S+)(\\s*)").matcher(input);

                        while (m.find())
                        {
                            String word = m.group(1);
                            String spaces = m.group(2);

                            if (word != null && !word.isEmpty())
                            {
                                inputWords.add(word.toLowerCase(Locale.ROOT));
                                wordCompleted.add(spaces != null && !spaces.isEmpty());
                            }
                        }
                    }

                    filteredResults.clear();
                    filteredIsSuggestion.clear();

                    ArrayList<ScoredItem> suggestions = new ArrayList<>();
                    ArrayList<ScoredItem> nonsuggestions = new ArrayList<>();

                    java.util.function.BiPredicate<String, String> containsFullWord = (text, word) ->
                            (" " + text + " ").contains(" " + word + " ");

                    java.util.function.BiPredicate<String, String> startsWithFullWord = (text, word) ->
                            text.startsWith(word + " ") || text.equals(word);

                    if (inputWords.isEmpty())
                    {
                        // Show only suggested items when nothing is typed
                        for (int idx = 0; idx < allItemNames.size(); idx++)
                        {
                            String s = allItemNames.get(idx);
                            String low = decodedItemNamesLower.get(idx);

                            boolean isSuggestion = false;

                            for (String keyword : suggestionKeywords)
                            {
                                if (!keyword.isEmpty() && startsWithFullWord.test(low, keyword))
                                {
                                    isSuggestion = true;
                                    break;
                                }
                            }

                            if (isSuggestion)
                            {
                                suggestions.add(new ScoredItem(s, 0, true));
                            }
                        }
                    }
                    else
                    {
                        for (int idx = 0; idx < allItemNames.size(); idx++)
                        {
                            String s = allItemNames.get(idx);
                            String low = decodedItemNamesLower.get(idx);
                            boolean allPresent = true;

                            for (int i = 0; i < inputWords.size(); ++i)
                            {
                                String w = inputWords.get(i);
                                boolean completed = wordCompleted.get(i);

                                if (completed)
                                {
                                    if (!containsFullWord.test(low, w)) { allPresent = false; break; }
                                }
                                else
                                {
                                    if (!low.contains(w))
                                    {
                                        allPresent = false; break;
                                    }
                                }
                            }

                            if (!allPresent) continue;

                            int score = 0;
                            StringBuilder inputJoinedBuilder = new StringBuilder();

                            for (int i = 0; i < inputWords.size(); ++i)
                            {
                                if (i > 0) inputJoinedBuilder.append(" ");
                                inputJoinedBuilder.append(inputWords.get(i));
                            }

                            String inputJoined = inputJoinedBuilder.toString();

                            if (!inputJoined.isEmpty() && low.contains(inputJoined)) score += 10;
                            if (!inputWords.isEmpty() && low.startsWith(inputWords.get(0))) score += 5;

                            int lastIdx = -1;
                            boolean inOrder = true;

                            for (String w : inputWords)
                            {
                                int idx2 = low.indexOf(w, lastIdx + 1);

                                if (idx2 == -1 || (lastIdx >= 0 && idx2 < lastIdx))
                                {
                                    inOrder = false;
                                    break;
                                }

                                lastIdx = idx2;
                            }

                            if (inOrder && inputWords.size() > 1) score += 2;

                            boolean isSuggestion = false;

                            for (String keyword : suggestionKeywords)
                            {
                                if (!keyword.isEmpty() && startsWithFullWord.test(low, keyword))
                                {
                                    isSuggestion = true;
                                    break;
                                }
                            }

                            ScoredItem item = new ScoredItem(s, score, isSuggestion);

                            if (isSuggestion)
                            {
                                suggestions.add(item);
                            }
                            else
                            {
                                nonsuggestions.add(item);
                            }
                        }

                        java.util.Comparator<ScoredItem> comp = (a, b) ->
                        {
                            int cmp = Integer.compare(b.score, a.score);
                            if (cmp != 0) return cmp;
                            return Baskit.decodeKey(a.item).compareToIgnoreCase(Baskit.decodeKey(b.item));
                        };

                        suggestions.sort(comp);
                        nonsuggestions.sort(comp);
                    }

                    // Sort suggestions by keyword priority
                    suggestions.sort((a, b) ->
                    {
                        String aLow = Baskit.decodeKey(a.item).toLowerCase(Locale.ROOT);
                        String bLow = Baskit.decodeKey(b.item).toLowerCase(Locale.ROOT);

                        int aKey = Integer.MAX_VALUE;
                        int bKey = Integer.MAX_VALUE;

                        for (int i = 0; i < suggestionKeywords.size(); i++) {
                            String k = suggestionKeywords.get(i);

                            if (aKey == Integer.MAX_VALUE && (aLow.startsWith(k + " ") || aLow.equals(k))) {
                                aKey = i;
                            }

                            if (bKey == Integer.MAX_VALUE && (bLow.startsWith(k + " ") || bLow.equals(k))) {
                                bKey = i;
                            }
                        }

                        int cmp = Integer.compare(aKey, bKey);
                        if (cmp != 0) return cmp;

                        return aLow.compareTo(bLow);
                    });
                    for (int i = 0; i < suggestions.size(); i++)
                    {
                        ScoredItem si = suggestions.get(i);
                        filteredResults.add(si.item);
                        filteredIsSuggestion.add(true);
                    }

                    for (int i = 0; i < nonsuggestions.size(); i++)
                    {
                        ScoredItem si = nonsuggestions.get(i);
                        filteredResults.add(si.item);
                        filteredIsSuggestion.add(false);
                    }

                    try
                    {
                        adapter.notifyDataSetChanged();
                    }
                    catch (Exception ignored) {}

                    searchItem.post(() ->
                    {
                        try
                        {
                            if (searchItem.hasFocus()) {
                                int itemCount = filteredResults.size();
                                int itemHeightPx = (int)(48 * fragmentView.getResources().getDisplayMetrics().density);
                                int desiredDropdownHeight = itemCount > 0 ? itemHeightPx * itemCount : ViewGroup.LayoutParams.WRAP_CONTENT;

                                android.graphics.Rect visibleFrame = new android.graphics.Rect();
                                fragmentView.getWindowVisibleDisplayFrame(visibleFrame);

                                int[] location = new int[2];
                                searchItem.getLocationOnScreen(location);

                                int viewBottom = location[1] + searchItem.getHeight();
                                int availableBelow = visibleFrame.bottom - viewBottom;

                                if (desiredDropdownHeight > 0 && desiredDropdownHeight < availableBelow)
                                {
                                    // Few items → let the dropdown size itself exactly
                                    searchItem.setDropDownHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                                }
                                else
                                {
                                    // Many items → limit by available screen space
                                    int finalDropdownHeight = Math.max(itemHeightPx, availableBelow);
                                    searchItem.setDropDownHeight(finalDropdownHeight);
                                }

                                searchItem.showDropDown();
                            }
                        }
                        catch (Exception e)
                        {
                            try
                            {
                                if (searchItem.hasFocus())
                                {
                                    searchItem.showDropDown();
                                }
                            }
                            catch (Exception ignored) {}
                        }
                    });
                };

                long FILTER_DEBOUNCE_DELAY = 60;
                searchItem.postDelayed(filterRunnable, FILTER_DEBOUNCE_DELAY);
            }

            class ScoredItem
            {
                final String item;
                final int score;
                final boolean isSuggestion;
                ScoredItem(String item, int score, boolean isSuggestion)
                {
                    this.item = item;
                    this.score = score;
                    this.isSuggestion = isSuggestion;
                }
            }
        });

        searchItem.setOnItemClickListener((parent, view, position, id) ->
        {
            String clickedName = Baskit.decodeKey((String) parent.getItemAtPosition(position));

            if (isBadItemName(clickedName))
            {
                searchItem.setError("שם פריט לא תקין");
                selectedItem = null;
                return;
            }

            selectedItem = new Item(clickedName);
            selectedItem.setQuantity(1);
            tvQuantity.setText("1");
            btnDown.setVisibility(View.INVISIBLE);
            recyclerSupermarkets.setAdapter(null);

            infoLayout.setVisibility(View.VISIBLE);
            searchItem.clearFocus();
            hideKeyboard();

            if (activity != null)
            {
                activity.runOnUiThread(() -> searchItem.dismissDropDown());
            }
            else
            {
                searchItem.post(() -> searchItem.dismissDropDown());
            }

            loadItemVariations(clickedName);
        });
    }

    private void loadItemVariations(String baseName)
    {
        if (groups == null || baseName == null) return;

        ArrayList<String> codes = groups.get(baseName);
        if (codes == null || codes.isEmpty()) return;

        activity.runWhenServerActive(() ->
        {
            ArrayList<ItemInfo> variations = new ArrayList<>();

            for (String code : codes)
            {
                try
                {
                    ItemInfo info = apiHandler.getItemInfo(code);

                    if (info != null && !variations.contains(info))
                    {
                        variations.add(info);
                    }
                }
                catch (Exception ignored) {}
            }

            activity.runOnUiThread(() ->
            {
                currentVariations.clear();
                currentVariations.addAll(variations);
                new VariationsManager(currentVariations,
                        chipGroupWeights,
                        chipGroupCompanies,
                        context,
                        this::applyVariationFilter).setupVariationFilters();
            });

        });
    }

    private void applyVariationFilter()
    {
        java.util.Set<String> selectedWeights = new java.util.HashSet<>();
        java.util.Set<String> selectedCompanies = new java.util.HashSet<>();

        for (int i = 0; i < chipGroupWeights.getChildCount(); i++)
        {
            Chip chip = (Chip) chipGroupWeights.getChildAt(i);
            if (chip.isChecked()) selectedWeights.add(chip.getText().toString());
        }

        for (int i = 0; i < chipGroupCompanies.getChildCount(); i++)
        {
            Chip chip = (Chip) chipGroupCompanies.getChildAt(i);
            if (chip.isChecked()) selectedCompanies.add(chip.getText().toString());
        }

        ArrayList<ItemInfo> matching = new ArrayList<>();

        for (ItemInfo info : currentVariations)
        {
            boolean weightMatch = selectedWeights.isEmpty() ||
                    selectedWeights.contains(info.getFullMeasureStr());

            boolean companyMatch = selectedCompanies.isEmpty() ||
                    (info.getCompany() != null && selectedCompanies.contains(info.getCompany()));

            if (weightMatch && companyMatch)
            {
                matching.add(info);
            }
        }

        if (selectedItem != null && selectedItem.getSupermarket() != null)
        {
            boolean stillValid = false;

            for (ItemInfo info : matching)
            {
                boolean sameWeight =
                        selectedItem.getInfo().getFullMeasureStr() != null &&
                        selectedItem.getInfo().getFullMeasureStr().equals(info.getFullMeasureStr());

                boolean sameCompany =
                        selectedItem.getCompany() != null &&
                        selectedItem.getCompany().equals(info.getCompany());

                if (sameWeight && sameCompany)
                {
                    stillValid = true;
                    break;
                }
            }

            // If the selected variation no longer exists after filtering → clear selection
            if (!stillValid)
            {
                selectedItem.setUnchosen();
            }
        }

        java.util.Set<String> availableWeights = new java.util.HashSet<>();

        for (ItemInfo info : currentVariations)
        {
            boolean companyMatch = selectedCompanies.isEmpty() ||
                    (info.getCompany() != null && selectedCompanies.contains(info.getCompany()));

            if (companyMatch && info.getWeight() > 0)
            {
                availableWeights.add(info.getFullMeasureStr());
            }
        }

        java.util.Set<String> availableCompanies = new java.util.HashSet<>();

        for (ItemInfo info : currentVariations)
        {
            boolean weightMatch = selectedWeights.isEmpty() ||
                    selectedWeights.contains(info.getFullMeasureStr());

            if (weightMatch && info.getCompany() != null && !info.getCompany().isEmpty())
            {
                availableCompanies.add(info.getCompany());
            }
        }

        for (int i = 0; i < chipGroupWeights.getChildCount(); i++)
        {
            Chip chip = (Chip) chipGroupWeights.getChildAt(i);
            String text = chip.getText().toString();

            boolean enabled = availableWeights.contains(text);
            chip.setEnabled(enabled);
            chip.setAlpha(enabled ? 1f : 0.3f);
        }

        for (int i = 0; i < chipGroupCompanies.getChildCount(); i++)
        {
            Chip chip = (Chip) chipGroupCompanies.getChildAt(i);
            String text = chip.getText().toString();

            boolean enabled = availableCompanies.contains(text);
            chip.setEnabled(enabled);
            chip.setAlpha(enabled ? 1f : 0.3f);
        }

        loadSupermarketPricesForVariations(matching);
    }


    private void hideKeyboard()
    {
        if (activity == null) return;

        activity.runOnUiThread(() ->
        {
            View focusView = activity.getCurrentFocus();
            View fallbackView = searchItem;
            View targetView = focusView != null ? focusView : fallbackView;

            if (targetView != null)
            {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (imm != null)
                {
                    imm.hideSoftInputFromWindow(targetView.getWindowToken(), 0);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setupButtons()
    {
        btnCancel.setOnClickListener(v -> dismiss());

        btnAddItem.setOnClickListener(v ->
        {
            if (selectedItem != null)
            {
                startProgressBar();
                try
                {
                    addItemInterface.addItem(selectedItem);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            else
            {
                searchItem.setError("בחר פריט מהרשימה");
            }
        });

        btnUp.setOnClickListener(v ->
        {
            if (selectedItem != null)
            {
                tvQuantity.setText(Integer.toString(selectedItem.raiseQuantity()));
                btnDown.setVisibility(View.VISIBLE);
            }
        });

        btnDown.setOnClickListener(v ->
        {
            if (selectedItem != null)
            {
                if (selectedItem.getQuantity() <= 1) return;

                int quantity = selectedItem.lowerQuantity();

                if (quantity == 1) btnDown.setVisibility(View.INVISIBLE);

                tvQuantity.setText(Integer.toString(quantity));
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadSupermarketPricesForVariations(ArrayList<ItemInfo> variations)
    {
        if (variations == null || variations.isEmpty())
        {
            recyclerSupermarkets.setAdapter(null);
            return;
        }

        activity.runWhenServerActive(() ->
        {
            ArrayList<ItemVariant> variants = new ArrayList<>();

            for (ItemInfo info : variations)
            {
                try
                {
                    Map<String, Map<String, Double>> data =
                            apiHandler.getItemPricesByCode(info.getCode());

                    if (data == null) continue;

                    for (Map.Entry<String, Map<String, Double>> entry : data.entrySet())
                    {
                        String supermarketName = entry.getKey();
                        Map<String, Double> sections = entry.getValue();
                        if (sections == null) continue;

                        for (Map.Entry<String, Double> sectionEntry : sections.entrySet())
                        {
                            String sectionName = sectionEntry.getKey();
                            Double priceObj = sectionEntry.getValue();
                            if (priceObj == null) continue;

                            Supermarket sm =
                                    new Supermarket(supermarketName, sectionName);

                            variants.add(
                                    new ItemVariant(
                                            sm,
                                            priceObj,
                                            info
                                    )
                            );
                        }
                    }
                }
                catch (Exception ignored) {}
            }

            // Sort variants by price (cheapest first)
            variants.sort(Comparator.comparingDouble(ItemVariant::getPrice));

            activity.runOnUiThread(() ->
            {
                pricesAdapter = new ItemViewPricesAdapter(
                        context,
                        variants,
                        (variant) ->
                        {
                            if (selectedItem == null) return;

                            if (variant == null)
                            {
                                selectedItem.setUnchosen();
                            }
                            else
                            {
                                selectedItem.fillVariant(variant);
                            }

                            if (pricesAdapter != null)
                            {
                                pricesAdapter.notifyDataSetChanged();
                            }
                        }
                );

                recyclerSupermarkets.setAdapter(pricesAdapter);
            });

        });
    }

    public void startProgressBar()
    {
        if (progressBar != null)
        {
            progressBar.setVisibility(View.VISIBLE);
            btnAddItem.setClickable(false);
            btnCancel.setClickable(false);
            btnUp.setClickable(false);
            btnDown.setClickable(false);
        }
    }

    public void endProgressBar()
    {
        if (progressBar != null)
        {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void updateData(ArrayList<String> newListItemNames)
    {
        listItemNames = newListItemNames;

        if (masterItemNames != null)
        {
            allItemNames = new ArrayList<>(masterItemNames);
        }

        init();
    }

    private boolean isBadItemName(String s)
    {
        if (s == null) return true;
        String t = s.trim();
        return t.isEmpty() || t.equalsIgnoreCase("null");
    }
}