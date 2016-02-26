package org.solovyev.android.calculator.plot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.solovyev.android.calculator.App;
import org.solovyev.android.calculator.AppComponent;
import org.solovyev.android.calculator.BaseDialogFragment;
import org.solovyev.android.calculator.R;
import org.solovyev.android.plotter.BasePlotterListener;
import org.solovyev.android.plotter.PlotFunction;
import org.solovyev.android.plotter.PlotIconView;
import org.solovyev.android.plotter.Plotter;
import org.solovyev.android.views.llm.DividerItemDecoration;
import org.solovyev.android.views.llm.LinearLayoutManager;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static android.support.v7.widget.LinearLayoutManager.VERTICAL;
import static android.view.Menu.NONE;

public class PlotFunctionsFragment extends BaseDialogFragment {

    @Inject
    Plotter plotter;
    @NonNull
    private final PlotterListener plotterListener = new PlotterListener();
    private Adapter adapter;

    public PlotFunctionsFragment() {
    }

    public static void show(@Nonnull FragmentManager fm) {
        App.showDialog(new PlotFunctionsFragment(), "plot-functions", fm);
    }

    @Override
    protected void inject(@NonNull AppComponent component) {
        super.inject(component);
        component.inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        plotter.addListener(plotterListener);
        return view;
    }

    @NonNull
    protected RecyclerView onCreateDialogView(@NonNull Context context, @NonNull LayoutInflater inflater, Bundle savedInstanceState) {
        @SuppressLint("InflateParams") final RecyclerView view = (RecyclerView) inflater.inflate(R.layout.dialog_functions, null);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(context, VERTICAL, false);
        final int itemHeight = context.getResources().getDimensionPixelSize(R.dimen.list_item_height);
        layoutManager.setChildSize(itemHeight + getDividerHeight(context));
        view.setLayoutManager(layoutManager);

        view.addItemDecoration(new DividerItemDecoration(context, null));
        adapter = new Adapter(plotter.getPlotData().functions);
        view.setAdapter(adapter);
        return view;
    }

    private int getDividerHeight(@NonNull Context context) {
        final TypedArray a = context.obtainStyledAttributes(null, new int[]{android.R.attr.listDivider});
        final Drawable divider = a.getDrawable(0);
        final int dividerHeight = divider == null ? 0 : divider.getIntrinsicHeight();
        a.recycle();
        return dividerHeight;
    }

    @Override
    public void onDestroyView() {
        plotter.removeListener(plotterListener);
        super.onDestroyView();
    }

    protected void onPrepareDialog(@NonNull AlertDialog.Builder builder) {
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNeutralButton("Add", null);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEUTRAL:
                PlotEditFunctionFragment.show(null, getActivity().getSupportFragmentManager());
                return;
            default:
                super.onClick(dialog, which);
                return;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {

        @Bind(R.id.function_icon)
        PlotIconView icon;

        @Bind(R.id.function_name)
        TextView name;
        private PlotFunction function;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        void bind(@NonNull PlotFunction function) {
            this.function = function;
            name.setText(function.function.getName());
            icon.setMeshSpec(function.meshSpec);
        }

        @Override
        public void onClick(View v) {
            PlotEditFunctionFragment.show(function, getActivity().getSupportFragmentManager());
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(NONE, R.string.c_remove, NONE, R.string.c_remove).setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (function != null && item.getItemId() == R.string.c_remove) {
                plotter.remove(function);
                return true;
            }
            return false;
        }
    }

    private class Adapter extends RecyclerView.Adapter {
        @NonNull
        private final List<PlotFunction> list;

        public Adapter(@NonNull List<PlotFunction> list) {
            this.list = list;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ViewHolder(inflater.inflate(R.layout.dialog_functions_function, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((ViewHolder) holder).bind(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void remove(@NonNull PlotFunction function) {
            final int i = list.indexOf(function);
            if (i >= 0) {
                list.remove(i);
                notifyItemRemoved(i);
            }
        }

        public void update(int id, @NonNull PlotFunction function) {
            final int i = find(id);
            if (i >= 0) {
                list.set(i, function);
                notifyItemChanged(i);
            }
        }

        private int find(int id) {
            for (int i = 0; i < list.size(); i++) {
                final PlotFunction function = list.get(i);
                if (function.function.getId() == id) {
                    return i;
                }
            }
            return -1;
        }

        public void add(@NonNull PlotFunction function) {
            list.add(function);
            notifyItemInserted(list.size() - 1);
        }
    }

    private class PlotterListener extends BasePlotterListener {
        @Override
        public void onFunctionAdded(@NonNull PlotFunction function) {
            adapter.add(function);
        }

        @Override
        public void onFunctionUpdated(int id, @NonNull PlotFunction function) {
            adapter.update(id, function);
        }

        @Override
        public void onFunctionRemoved(@NonNull PlotFunction function) {
            adapter.remove(function);
        }
    }
}
