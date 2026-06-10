CREATE INDEX idx_posts_user_created  ON posts(user_id, created_at DESC);
CREATE INDEX idx_posts_created       ON posts(created_at DESC);
CREATE INDEX idx_comments_post       ON comments(post_id, created_at ASC);
CREATE INDEX idx_comments_parent     ON comments(parent_id);
CREATE INDEX idx_todos_user_date     ON todos(user_id, date DESC);
CREATE INDEX idx_follows_follower    ON follows(follower_id);
CREATE INDEX idx_follows_following   ON follows(following_id);
CREATE INDEX idx_post_tags_tag       ON post_tags(tag_id);
CREATE INDEX idx_tags_name           ON tags(name text_pattern_ops);
