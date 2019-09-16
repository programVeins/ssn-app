package in.edu.ssn.ssnapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.SnapshotParser;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.hendraanggrian.appcompat.widget.SocialTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import in.edu.ssn.ssnapp.adapters.ClubPostImageAdapter;
import in.edu.ssn.ssnapp.models.Club;
import in.edu.ssn.ssnapp.models.ClubPost;
import in.edu.ssn.ssnapp.models.Comments;
import in.edu.ssn.ssnapp.utils.SharedPref;
import spencerstudios.com.bungeelib.Bungee;

public class AnimationActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener, View.OnClickListener {

    private boolean mIsTheTitleVisible = false;
    private boolean mIsTheTitleContainerVisible = true;

    private Toolbar toolbar;
    private RelativeLayout layout_page_detail;
    private LottieAnimationView lottie;

    private FirestoreRecyclerAdapter adapter;
    private ShimmerFrameLayout shimmer_view;
    private RecyclerView feedsRV;
    private Club club;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animation);

        initUI();

        setupFireStore();

        /****************************************************/
    }

    private void initUI(){
        //Toolbar

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        ImageView tool_iv_back = findViewById(R.id.tool_iv_back);
        CircleImageView tool_iv_dp = findViewById(R.id.tool_iv_dp);
        TextView tool_tv_title = findViewById(R.id.tool_tv_title);

        setSupportActionBar(toolbar);
        startAlphaAnimation(toolbar,0, View.INVISIBLE);

        //Collapsible layout

        AppBarLayout layout_app_bar = findViewById(R.id.layout_app_bar);    layout_app_bar.addOnOffsetChangedListener(this);
        layout_page_detail = findViewById(R.id.layout_page_detail);

        ImageView backIV = findViewById(R.id.backIV);                       backIV.setOnClickListener(this);
        ImageView iv_cover_pic = findViewById(R.id.iv_cover_pic);
        CircleImageView iv_dp_pic = findViewById(R.id.iv_dp_pic);

        TextView tv_following_text = findViewById(R.id.tv_following_text);
        lottie = findViewById(R.id.lottie);                                 lottie.setOnClickListener(this);

        TextView tv_title = findViewById(R.id.tv_title);
        TextView tv_description = findViewById(R.id.tv_description);
        TextView tv_contact = findViewById(R.id.tv_contact);                tv_contact.setOnClickListener(this);

        TextView tv_followers = findViewById(R.id.tv_followers);
        TextView tv_followers_text = findViewById(R.id.tv_followers_text);

        shimmer_view = findViewById(R.id.shimmer_view);
        feedsRV = findViewById(R.id.feedsRV);

        LinearLayoutManager layoutManager = new LinearLayoutManager(AnimationActivity.this);
        feedsRV.setLayoutManager(layoutManager);

        /****************************************************/
        //Update Data

        club = getIntent().getParcelableExtra("data");

        if(club.getFollowers().contains(SharedPref.getString(getApplicationContext(),"email"))){
            tv_following_text.setText("Following");
            tv_following_text.setTextColor(Color.BLACK);
        }
        else{
            tv_following_text.setText("Unfollowing");
            tv_following_text.setTextColor(getResources().getColor(R.color.light_grey));
        }

        lottie.addAnimatorUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (valueAnimator.isRunning()) {
                    if(!club.getFollowers().contains(SharedPref.getString(getApplicationContext(),"email"))){
                        if (lottie.getProgress() > 0.50) {
                            lottie.setProgress(0.0f);
                            lottie.pauseAnimation();
                        }
                    }
                    else if(lottie.getProgress() > 0.6)
                        lottie.pauseAnimation();
                }
            }
        });

        tv_title.setText(club.getName());
        tool_tv_title.setText(club.getName());

        tv_description.setText(club.getDescription());
        tv_contact.setText(club.getContact());

        if(club.getFollowers().size()>0){
            tv_followers.setText(club.getFollowers().size() + "");
            tv_followers_text.setText("Followers");
        }
        else{
            tv_followers.setText("0");
            tv_followers_text.setText("Follower");
        }

        Glide.with(this).load(club.getDp_url()).placeholder(R.color.shimmering_front).into(iv_dp_pic);
        Glide.with(this).load(club.getDp_url()).placeholder(R.color.shimmering_front).into(tool_iv_dp);
        Glide.with(this).load(club.getCover_url()).placeholder(R.color.shimmering_back).into(iv_cover_pic);
    }

    private void setupFireStore() {
        final TextDrawable.IBuilder builder = TextDrawable.builder()
                .beginConfig()
                .toUpperCase()
                .endConfig()
                .round();

        Query query = FirebaseFirestore.getInstance().collection("post_club").whereEqualTo("cid",club.getId()).orderBy("time", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<ClubPost> options = new FirestoreRecyclerOptions.Builder<ClubPost>().setQuery(query, new SnapshotParser<ClubPost>() {
            @NonNull
            @Override
            public ClubPost parseSnapshot(@NonNull DocumentSnapshot snapshot) {
                shimmer_view.setVisibility(View.VISIBLE);

                final ClubPost post = new ClubPost();
                post.setId(snapshot.getString("id"));
                post.setCid(snapshot.getString("cid"));
                post.setAuthor(snapshot.getString("author"));
                post.setTitle(snapshot.getString("title"));
                post.setDescription(snapshot.getString("description"));
                post.setTime(snapshot.getTimestamp("time").toDate());

                ArrayList<String> like = (ArrayList<String>) snapshot.get("like");
                if(like != null && like.size() > 0)
                    post.setLike(like);
                else
                    post.setLike(null);

                ArrayList<Comments> comments = (ArrayList<Comments>) snapshot.get("comment");
                if(comments != null && comments.size() > 0)
                    post.setComment(comments);
                else
                    post.setComment(null);

                ArrayList<String> images = (ArrayList<String>) snapshot.get("img_urls");
                if(images != null && images.size() > 0)
                    post.setImg_urls(images);
                else
                    post.setImg_urls(null);

                try {
                    ArrayList<Map<String, String>> files = (ArrayList<Map<String, String>>) snapshot.get("file_urls");
                    if (files != null && files.size() != 0) {
                        ArrayList<String> fileName = new ArrayList<>();
                        ArrayList<String> fileUrl = new ArrayList<>();

                        for (int i = 0; i < files.size(); i++) {
                            fileName.add(files.get(i).get("name"));
                            fileUrl.add(files.get(i).get("url"));
                        }
                        post.setFileName(fileName);
                        post.setFileUrl(fileUrl);
                    } else {
                        post.setFileName(null);
                        post.setFileUrl(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Crashlytics.log("stackTrace: " + Arrays.toString(e.getStackTrace()) + " \n Error: " + e.getMessage());
                    post.setFileName(null);
                    post.setFileUrl(null);
                }
                return post;
            }
        }).build();

        adapter = new FirestoreRecyclerAdapter<ClubPost, ClubPageActivity.FeedViewHolder>(options) {
            @Override
            public void onBindViewHolder(final ClubPageActivity.FeedViewHolder holder, final int position, final ClubPost model) {
                String author = "";
                String email = model.getAuthor();
                email = email.substring(0, email.indexOf("@"));
                for (int j = 0; j < email.length(); j++) {
                    if (Character.isDigit(email.charAt(j))) {
                        author = email.substring(0, j);
                        break;
                    }
                }
                if (author.isEmpty()) {
                    author = email;
                }
                holder.tv_author.setText(author);
                holder.tv_title.setText(model.getTitle());

                ColorGenerator generator = ColorGenerator.MATERIAL;
                int color = generator.getColor(model.getAuthor());
                TextDrawable ic1 = builder.build(String.valueOf(model.getAuthor().charAt(0)), color);
                holder.userImageIV.setImageDrawable(ic1);

                if (model.getLike().contains(SharedPref.getString(getApplicationContext(), "email"))) {
                    holder.like.setImageResource(R.drawable.blue_heart);
                }
                else {
                    holder.like.setImageResource(R.drawable.heart);
                }

                Date time = model.getTime();
                Date now = new Date();
                Long t = now.getTime() - time.getTime();
                String timer;

                if (t < 60000)
                    timer = Long.toString(t / 1000) + "s ago";
                else if (t < 3600000)
                    timer = Long.toString(t / 60000) + "m ago";
                else if (t < 86400000)
                    timer = Long.toString(t / 3600000) + "h ago";
                else if (t < 604800000)
                    timer = Long.toString(t / 86400000) + "d ago";
                else if (t < 2592000000L)
                    timer = Long.toString(t / 604800000) + "w ago";
                else if (t < 31536000000L)
                    timer = Long.toString(t / 2592000000L) + "M ago";
                else
                    timer = Long.toString(t / 31536000000L) + "y ago";

                holder.tv_time.setText(timer);

                if (model.getDescription().length() > 100) {
                    SpannableString ss = new SpannableString(model.getDescription().substring(0, 100) + "... see more");
                    ss.setSpan(new RelativeSizeSpan(0.9f), ss.length() - 12, ss.length(), 0);
                    ss.setSpan(new ForegroundColorSpan(Color.parseColor("#404040")), ss.length() - 12, ss.length(), 0);
                    holder.tv_description.setText(ss);
                }
                else
                    holder.tv_description.setText(model.getDescription().trim());

                if (model.getImg_urls() != null && model.getImg_urls().size() != 0) {
                    holder.viewPager.setVisibility(View.VISIBLE);

                    final ClubPostImageAdapter imageAdapter = new ClubPostImageAdapter(AnimationActivity.this, model.getImg_urls(), true, model, timer);
                    holder.viewPager.setAdapter(imageAdapter);

                    if (model.getImg_urls().size() == 1) {
                        holder.tv_current_image.setVisibility(View.GONE);
                    }
                    else {
                        holder.tv_current_image.setVisibility(View.VISIBLE);
                        holder.tv_current_image.setText(String.valueOf(1) + " / " + String.valueOf(model.getImg_urls().size()));
                        holder.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                            @Override
                            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                            }

                            @Override
                            public void onPageSelected(int pos) {
                                holder.tv_current_image.setText(String.valueOf(pos + 1) + " / " + String.valueOf(model.getImg_urls().size()));
                            }

                            @Override
                            public void onPageScrollStateChanged(int state) {

                            }
                        });
                    }
                } else {
                    holder.viewPager.setVisibility(View.GONE);
                    holder.tv_current_image.setVisibility(View.GONE);
                }

                holder.like.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!model.getLike().contains(SharedPref.getString(getApplicationContext(), "email"))) {
                            holder.like.setImageResource(R.drawable.blue_heart);
                            FirebaseFirestore.getInstance().collection("post_club").document(model.getId()).update("like", FieldValue.arrayUnion(SharedPref.getString(getApplicationContext(),"email")));
                        }
                        else {
                            holder.like.setImageResource(R.drawable.heart);{
                                model.getLike().remove(SharedPref.getString(getApplicationContext(), "email"));
                                FirebaseFirestore.getInstance().collection("post_club").document(model.getId()).update("like", FieldValue.arrayRemove(SharedPref.getString(getApplicationContext(),"email")));
                            }
                        }
                    }
                });
                try {
                    holder.comment_count.setText(Integer.toString(model.getComment().size()));
                    holder.like_count.setText(Integer.toString(model.getLike().size()));
                }
                catch (Exception e){
                    e.printStackTrace();
                }

                holder.feed_view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), ClubPostDetailsActivity.class);
                        intent.putExtra("data", model);
                        startActivity(intent);
                        Bungee.slideLeft(AnimationActivity.this);
                    }
                });

                shimmer_view.setVisibility(View.GONE);
            }

            @NonNull
            @Override
            public ClubPageActivity.FeedViewHolder onCreateViewHolder(@NonNull ViewGroup group, int i) {
                View view = LayoutInflater.from(AnimationActivity.this).inflate(R.layout.club_post_item, group, false);
                return new ClubPageActivity.FeedViewHolder(view);
            }
        };

        feedsRV.setAdapter(adapter);
    }

    public static class FeedViewHolder extends RecyclerView.ViewHolder {
        public TextView tv_author, tv_club, tv_title, tv_time, tv_current_image, like_count, comment_count;
        public SocialTextView tv_description;
        public ImageView userImageIV, like, comment;
        public RelativeLayout feed_view;
        public ViewPager viewPager;

        public FeedViewHolder(View itemView) {
            super(itemView);

            tv_author = itemView.findViewById(R.id.tv_author_club);
            tv_title = itemView.findViewById(R.id.tv_title_club);
            tv_description = itemView.findViewById(R.id.tv_description_club);
            tv_time = itemView.findViewById(R.id.tv_time_club);
            tv_current_image = itemView.findViewById(R.id.currentImageTV_club);
            userImageIV = itemView.findViewById(R.id.userImageIV_club);
            feed_view = itemView.findViewById(R.id.feed_view_club);
            viewPager = itemView.findViewById(R.id.viewPager_club);
            like = itemView.findViewById(R.id.like_IV_club);
            comment = itemView.findViewById(R.id.comment_IV_club);
            like_count = itemView.findViewById(R.id.like_count_tv);
            comment_count = itemView.findViewById(R.id.comment_count_tv);
        }
    }

    /****************************************************/

    @Override
    public void onClick(View view) {

    }

    /****************************************************/
    //Transition effects

    public static void startAlphaAnimation(View v, long duration, int visibility) {
        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE) ? new AlphaAnimation(0f, 1f) : new AlphaAnimation(1f, 0f);
        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        v.startAnimation(alphaAnimation);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(offset) / (float) maxScroll;

        handleAlphaOnTitle(percentage);
        handleToolbarTitleVisibility(percentage);
    }

    private void handleToolbarTitleVisibility(float percentage) {
        if (percentage >= 0.8f) {
            if (!mIsTheTitleVisible) {
                startAlphaAnimation(toolbar, 200, View.VISIBLE);
                mIsTheTitleVisible = true;
            }

        }
        else {
            if (mIsTheTitleVisible) {
                startAlphaAnimation(toolbar, 200, View.INVISIBLE);
                mIsTheTitleVisible = false;
            }
        }
    }

    private void handleAlphaOnTitle(float percentage) {
        if (percentage >= 0.8f) {
            if (mIsTheTitleContainerVisible) {
                startAlphaAnimation(layout_page_detail, 200, View.INVISIBLE);
                mIsTheTitleContainerVisible = false;
            }

        }
        else {
            if (!mIsTheTitleContainerVisible) {
                startAlphaAnimation(layout_page_detail, 200, View.VISIBLE);
                mIsTheTitleContainerVisible = true;
            }
        }
    }

    /****************************************************/
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Bungee.slideRight(AnimationActivity.this);
    }
}

//Black color Following
//placeholder change shimmering
//share & back button