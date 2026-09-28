-- PostgreSQL Schema for Kalo Cloud Sync

-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- User Profiles
create table if not exists public.profiles (
    id uuid references auth.users on delete cascade primary key,
    daily_calorie_target integer default 2200,
    daily_protein_target integer default 160,
    daily_carbs_target integer default 220,
    daily_fat_target integer default 70,
    daily_step_goal integer default 10000,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Meals
create table if not exists public.meals (
    id text primary key,
    user_id uuid references auth.users on delete cascade not null,
    title text not null,
    total_calories integer not null,
    total_protein numeric not null,
    total_carbs numeric not null,
    total_fat numeric not null,
    image_remote_url text,
    notes text,
    logged_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Food Items breakdown
create table if not exists public.food_items (
    id text primary key,
    meal_id text references public.meals on delete cascade not null,
    name text not null,
    portion_grams numeric not null,
    calories integer not null,
    protein numeric not null,
    carbs numeric not null,
    fat numeric not null,
    confidence numeric default 1.0
);

-- Workouts
create table if not exists public.workouts (
    id text primary key,
    user_id uuid references auth.users on delete cascade not null,
    title text not null,
    type text not null, -- 'STRENGTH', 'CARDIO'
    duration_minutes integer default 0,
    estimated_calories_burned integer default 0,
    logged_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Exercise Sets
create table if not exists public.exercise_sets (
    id text primary key,
    workout_id text references public.workouts on delete cascade not null,
    exercise_name text not null,
    set_number integer not null,
    weight_kg numeric not null,
    reps integer not null,
    is_completed boolean default true
);

-- Row Level Security (RLS)
alter table public.profiles enable row level security;
alter table public.meals enable row level security;
alter table public.food_items enable row level security;
alter table public.workouts enable row level security;
alter table public.exercise_sets enable row level security;

create policy "Users can view their own profile" on public.profiles for select using (auth.uid() = id);
create policy "Users can insert their own profile" on public.profiles for insert with check (auth.uid() = id);
create policy "Users can update their own profile" on public.profiles for update using (auth.uid() = id);

create policy "Users can view their own meals" on public.meals for select using (auth.uid() = user_id);
create policy "Users can insert their own meals" on public.meals for insert with check (auth.uid() = user_id);
create policy "Users can delete their own meals" on public.meals for delete using (auth.uid() = user_id);

create policy "Users can view their food items" on public.food_items for select using (
    exists (select 1 from public.meals where public.meals.id = public.food_items.meal_id and public.meals.user_id = auth.uid())
);
create policy "Users can insert their food items" on public.food_items for insert with check (
    exists (select 1 from public.meals where public.meals.id = public.food_items.meal_id and public.meals.user_id = auth.uid())
);
create policy "Users can delete their food items" on public.food_items for delete using (
    exists (select 1 from public.meals where public.meals.id = public.food_items.meal_id and public.meals.user_id = auth.uid())
);

create policy "Users can view their own workouts" on public.workouts for select using (auth.uid() = user_id);
create policy "Users can insert their own workouts" on public.workouts for insert with check (auth.uid() = user_id);
create policy "Users can delete their own workouts" on public.workouts for delete using (auth.uid() = user_id);

create policy "Users can view their exercise sets" on public.exercise_sets for select using (
    exists (select 1 from public.workouts where public.workouts.id = public.exercise_sets.workout_id and public.workouts.user_id = auth.uid())
);
create policy "Users can insert their exercise sets" on public.exercise_sets for insert with check (
    exists (select 1 from public.workouts where public.workouts.id = public.exercise_sets.workout_id and public.workouts.user_id = auth.uid())
);
create policy "Users can delete their exercise sets" on public.exercise_sets for delete using (
    exists (select 1 from public.workouts where public.workouts.id = public.exercise_sets.workout_id and public.workouts.user_id = auth.uid())
);
