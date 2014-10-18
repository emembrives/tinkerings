package fr.membrives.dispotrains.adapters;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import fr.membrives.dispotrains.R;
import fr.membrives.dispotrains.data.Elevator;

public class ElevatorAdapter extends ArrayAdapter<Elevator> {
    Context context;

    public ElevatorAdapter(Context context, List<Elevator> objects) {
        super(context, 0, objects);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Elevator elevator = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.elevator_list_item,
                    parent, false);
        }
        ((TextView) convertView.findViewById(R.id.situation)).setText(elevator.getSituation());
        ((TextView) convertView.findViewById(R.id.direction)).setText(elevator.getDirection());
        ((TextView) convertView.findViewById(R.id.id)).setText(elevator.getId());
        ((TextView) convertView.findViewById(R.id.status)).setText(elevator.getStatusDescription());

        TextView dateView = (TextView) convertView.findViewById(R.id.date);
        Date date = elevator.getStatusDate();
        String dateStr = DateFormat.getMediumDateFormat(context).format(date);
        dateView.setText(dateStr);

        if (!elevator.getStatusDescription().equalsIgnoreCase("Disponible")) {
            convertView.setBackgroundColor(getContext().getResources().getColor(R.color.problem));
        }
        // Return the completed view to render on screen
        return convertView;
    }
}
