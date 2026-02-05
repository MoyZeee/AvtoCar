public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {
    private List<Car> carList;
    private Context context;

    public CarAdapter(List<Car> carList, Context context) {
        this.carList = carList;
        this.context = context;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.car_item, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);

        holder.carModel.setText(car.getModel());
        holder.carPrice.setText("Цена: " + car.getPrice() + "/мин");
        holder.carAvailability.setText(car.getAvailability());
        holder.carImage.setImageResource(car.getImageResource());

        holder.rentButton.setOnClickListener(v -> {
            // Обработка нажатия кнопки аренды
            Intent intent = new Intent(context, RentActivity.class);
            intent.putExtra("car_model", car.getModel());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView carImage;
        TextView carModel;
        TextView carPrice;
        TextView carAvailability;
        Button rentButton;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            carImage = itemView.findViewById(R.id.carImage);
            carModel = itemView.findViewById(R.id.carModel);
            carPrice = itemView.findViewById(R.id.carPrice);
            carAvailability = itemView.findViewById(R.id.carAvailability);
            rentButton = itemView.findViewById(R.id.rentButton);
        }
    }
}