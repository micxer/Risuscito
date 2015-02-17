package it.cammino.risuscito;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.internal.widget.TintEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import it.cammino.risuscito.utils.ThemeUtils;
import it.cammino.utilities.material.PaperButton;

public class InsertVeloceFragment extends Fragment {

    private DatabaseCanti listaCanti;
    private List<CantoItem> titoli;
    private TintEditText searchPar;
    private View rootView;
    RecyclerView recyclerView;
    CantoRecyclerAdapter cantoAdapter;

    private int fromAdd;
    private int idLista;
    private int listPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(
                R.layout.activity_ricerca_titolo, container, false);

        searchPar = (TintEditText) rootView.findViewById(R.id.textfieldRicerca);
        listaCanti = new DatabaseCanti(getActivity());

        recyclerView = (RecyclerView) rootView.findViewById(R.id.matchedList);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // recupera il titolo della voce cliccata
                String cantoCliccato = ((TextView) v.findViewById(R.id.text_title))
                        .getText().toString();
                String cantoCliccatoNoApex = Utility.duplicaApostrofi(cantoCliccato);

                SQLiteDatabase db = listaCanti.getReadableDatabase();

                if (fromAdd == 1)  {
                    // chiamato da una lista predefinita
                    String query = "SELECT _id" +
                            "  FROM ELENCO" +
                            "  WHERE titolo =  '" + cantoCliccatoNoApex + "'";
                    Cursor cursor = db.rawQuery(query, null);

                    // recupera il nome del file
                    cursor.moveToFirst();
                    int idCanto = cursor.getInt(0);

                    // chiude il cursore
                    cursor.close();

                    String sql = "INSERT INTO CUST_LISTS ";
                    sql+= "VALUES (" + idLista + ", "
                            + listPosition + ", "
                            + idCanto
                            + ", CURRENT_TIMESTAMP)";

                    try {
                        db.execSQL(sql);
                    } catch (SQLException e) {
                        Toast toast = Toast.makeText(getActivity()
                                , getString(R.string.present_yet), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                else {
                    //chiamato da una lista personalizzata
                    String query = "SELECT lista" +
                            "  FROM LISTE_PERS" +
                            "  WHERE _id =  " + idLista;
                    Cursor cursor = db.rawQuery(query, null);

                    // recupera l'oggetto lista personalizzata
                    cursor.moveToFirst();

                    ListaPersonalizzata listaPersonalizzata = (ListaPersonalizzata) ListaPersonalizzata.
                            deserializeObject(cursor.getBlob(0));

                    // chiude il cursore
                    cursor.close();

                    // lancia la ricerca di tutti i titoli presenti in DB e li dispone in ordine alfabetico
                    query = "SELECT color, pagina" +
                            "		FROM ELENCO" +
                            "		WHERE titolo = '" + cantoCliccatoNoApex + "'";
                    cursor = db.rawQuery(query, null);

                    cursor.moveToFirst();

                    listaPersonalizzata.addCanto(Utility.intToString(cursor.getInt(1), 3) + cursor.getString(0) + cantoCliccato, listPosition);

                    ContentValues  values = new  ContentValues( );
                    values.put("lista" , ListaPersonalizzata.serializeObject(listaPersonalizzata));
                    db.update("LISTE_PERS", values, "_id = " + idLista, null );
                    db.close();
                }

                getActivity().finish();
                getActivity().overridePendingTransition(0, R.anim.slide_out_right);
            }
        };

        // Creating new adapter object
        titoli = new ArrayList<CantoItem>();
        cantoAdapter = new CantoRecyclerAdapter(titoli, clickListener);
        recyclerView.setAdapter(cantoAdapter);

        // Setting the layoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        Bundle bundle = getArguments();
        fromAdd = bundle.getInt("fromAdd");
        idLista = bundle.getInt("idLista");
        listPosition = bundle.getInt("position");

        searchPar.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String tempText = ((EditText) getActivity().findViewById(R.id.tempTextField)).getText().toString();
                if (!tempText.equals(s.toString()))
                    ((EditText) getActivity().findViewById(R.id.tempTextField)).setText(s);

                if (s.length() >= 3) {

                    rootView.findViewById(R.id.search_no_results).setVisibility(View.GONE);

                    String titolo = Utility.duplicaApostrofi(s.toString());

                    // crea un manipolatore per il Database in modalità READ
                    SQLiteDatabase db = listaCanti.getReadableDatabase();

                    // lancia la ricerca di tutti i titoli presenti in DB e li dispone in ordine alfabetico
                    String query = "SELECT titolo, color, pagina" +
                            "		FROM ELENCO" +
                            "		WHERE titolo like '%" + titolo + "%'" +
                            "		ORDER BY titolo ASC";
                    Cursor lista = db.rawQuery(query, null);

                    //recupera il numero di record trovati
                    int total = lista.getCount();

                    // crea un array e ci memorizza i titoli estratti
                    titoli.clear();
                    lista.moveToFirst();
                    for (int i = 0; i < total; i++) {
                        titoli.add(new CantoItem(Utility.intToString(lista.getInt(2), 3)
                                + lista.getString(1) + lista.getString(0)));
                        lista.moveToNext();
                    }

                    // chiude il cursore
                    lista.close();
                    cantoAdapter.notifyDataSetChanged();


                    if (total == 0)
                        rootView.findViewById(R.id.search_no_results).setVisibility(View.VISIBLE);
                }
                else {
                    if (s.length() == 0) {
                        titoli.clear();
                        cantoAdapter.notifyDataSetChanged();
                        rootView.findViewById(R.id.search_no_results).setVisibility(View.GONE);
                    }
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) { }

        });

        searchPar.setOnEditorActionListener(new TintEditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //to hide soft keyboard
                    ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(searchPar.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        ((EditText) getActivity().findViewById(R.id.tempTextField)).addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                String tempText = searchPar.getText().toString();
                if (!tempText.equals(s.toString()))
                    searchPar.setText(s);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) { }

        });

        PaperButton paperPulisci = (PaperButton) rootView.findViewById(R.id.pulisci_ripple);
        paperPulisci.setColor(getThemeUtils().primaryColor());
        paperPulisci.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchPar.setText("");
                rootView.findViewById(R.id.search_no_results).setVisibility(View.GONE);
            }
        });

        return rootView;
    }

    @Override
    public void onDestroy() {
        if (listaCanti != null)
            listaCanti.close();
        super.onDestroy();
    }

    private ThemeUtils getThemeUtils() {
        return ((GeneralInsertSearch)getActivity()).getThemeUtils();
    }

}